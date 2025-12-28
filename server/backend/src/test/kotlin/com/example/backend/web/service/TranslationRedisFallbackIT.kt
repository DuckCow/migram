package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.BoardRepository
import com.example.backend.web.repository.ContentTranslationRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

/**
 * Custom PostgreSQL container class to resolve Kotlin generic type issues
 * This is the recommended pattern from Testcontainers documentation for Kotlin
 */
class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)

/**
 * Integration test for TranslationService DB caching behavior
 *
 * Objective: Verify that TranslationService uses DB-only caching (without Redis dependency)
 *
 * Architecture Note:
 * - TranslationService does NOT use Spring Cache (@Cacheable)
 * - Uses DB-only caching via contentTranslationRepository
 * - Redis is intentionally NOT configured for this service (avoids external dependency)
 *
 * Test scenarios:
 * 1. Service works without Redis configuration
 * 2. Translation saved to DB cache
 * 3. Translation API called only once (second request uses DB cache)
 * 4. Same source/target language returns original (no API call)
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TranslationRedisFallbackIT {

    companion object {
        // MockWebServer for mocking translation API - initialized lazily
        private val mockWebServer: MockWebServer by lazy {
            MockWebServer().apply { start() }
        }

        @Container
        @JvmField
        val postgres: KPostgreSQLContainer = KPostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @AfterAll
        fun tearDown() {
            println("=== Starting test cleanup to prevent worker JVM hang ===")

            // 1. Shutdown MockWebServer with timeout to prevent hang
            try {
                val shutdownThread = Thread {
                    try {
                        mockWebServer.shutdown()
                        println("MockWebServer shutdown complete")
                    } catch (e: Exception) {
                        System.err.println("MockWebServer shutdown error: ${e.message}")
                    }
                }
                shutdownThread.start()
                shutdownThread.join(3000) // Wait max 3 seconds
                if (shutdownThread.isAlive) {
                    System.err.println("MockWebServer shutdown timeout - forcing interrupt")
                    shutdownThread.interrupt()
                }
            } catch (e: Exception) {
                System.err.println("MockWebServer teardown failed: ${e.message}")
            }

            println("=== Test cleanup complete ===")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // ========== PostgreSQL (Dynamic - Testcontainers) ==========
            // Override application-test.yml H2 config with Testcontainers PostgreSQL
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.PostgreSQLDialect" }

            // ========== Redis (Not Used by TranslationService) ==========
            // TranslationService uses DB-only caching (contentTranslationRepository)
            // No need to configure Redis for this test
            // Note: spring.cache.type=none (from application-test.yml) disables RedisCacheConfig

            // ========== Translation Service (Dynamic - MockWebServer) ==========
            // Point to MockWebServer (lazy initialization ensures it's started)
            val mockServerUrl = mockWebServer.url("/").toString().trimEnd('/')
            println("=== MockWebServer started at: $mockServerUrl ===")
            registry.add("translation.base-url") { mockServerUrl }
        }
    }

    @Autowired
    private lateinit var translationService: TranslationService

    @Autowired
    private lateinit var contentTranslationRepository: ContentTranslationRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var boardRepository: BoardRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var dataSource: DataSource

    private lateinit var testPost: Post

    @BeforeEach
    fun setupTestData() {
        println("=== Starting @BeforeEach setup ===")

        // Clear database before each test
        contentTranslationRepository.deleteAll()
        postRepository.deleteAll()
        userRepository.deleteAll()
        boardRepository.deleteAll()

        // Clear any remaining mock responses from previous tests
        // Drain all pending requests with timeout to prevent infinite wait
        println("Draining MockWebServer pending requests...")
        var drainedCount = 0
        var request = Companion.mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)
        while (request != null) {
            drainedCount++
            request = Companion.mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)
        }
        println("Drained $drainedCount pending requests from MockWebServer")

        // Create test board
        val board = boardRepository.save(
            Board(
                name = "Test Board",
                description = "Test board description",
                boardType = BoardType.COUNTRY,
                context = "KR",
                status = BoardStatus.ACTIVE
            )
        )

        // Create test user
        val user = userRepository.save(
            User(
                email = "test@example.com",
                name = "Test User",
                profilePicture = null,
                originCountry = null,
                currentCountry = null
            )
        )

        // Create test post
        testPost = postRepository.save(
            Post(
                title = "Hello World",
                content = "This is a test post in English.",
                board = board,
                user = user,
                originalLang = "en"
            )
        )

        // Prepare mock responses for translation API
        // Enqueue 2 responses as safety net (DB cache should prevent 2nd API call)
        val mockResponse = MockResponse()
            .setBody("""{"translatedText": "안녕하세요 세계\n\n이것은 영어로 된 테스트 게시물입니다.", "model": "opus-mt-en-ko"}""")
            .addHeader("Content-Type", "application/json")
        Companion.mockWebServer.enqueue(mockResponse)
        println("Enqueued mock response #1")

        // Safety net: If DB caching fails, 2nd request won't hang
        // Test will fail on requestCount assertion instead
        Companion.mockWebServer.enqueue(mockResponse)
        println("Enqueued mock response #2 (safety net)")
        println("=== @BeforeEach setup complete ===")
        println("")
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    @DisplayName("Service works with DB-only caching (no Redis dependency)")
    fun testDBCache_ServiceWorksWithoutErrors() {
        // Given: Spring Cache is disabled (cache.type=none)
        // When: Request translation for the first time
        val request = TranslationRequestDTO(
            contentType = ContentType.POST,
            contentId = testPost.id,
            targetLang = "ko"
        )

        // Then: Should work without errors (no Redis required)
        assertDoesNotThrow {
            val result = translationService.getTranslation(request)

            // Verify result is correct
            assertThat(result.contentType).isEqualTo(ContentType.POST)
            assertThat(result.contentId).isEqualTo(testPost.id)
            assertThat(result.sourceLang).isEqualTo("en")
            assertThat(result.targetLang).isEqualTo("ko")
            assertThat(result.translatedText).contains("안녕하세요")
            assertThat(result.model).isEqualTo("opus-mt-en-ko")
            assertThat(result.cached).isFalse()  // First call is not cached
        }
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    @DisplayName("Translation should be saved to DB cache")
    fun testDBCache_TranslationSavedToDatabase() {
        // Given: Service uses DB-only caching
        val request = TranslationRequestDTO(
            contentType = ContentType.POST,
            contentId = testPost.id,
            targetLang = "ko"
        )

        // When: Request translation
        translationService.getTranslation(request)

        // Then: Translation should be saved to content_translation table
        val savedTranslation = contentTranslationRepository
            .findByContentTypeAndContentIdAndTargetLang(ContentType.POST, testPost.id, "ko")

        assertThat(savedTranslation).isPresent
        assertThat(savedTranslation.get().translatedText).contains("안녕하세요")
        assertThat(savedTranslation.get().model).isEqualTo("opus-mt-en-ko")
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    @DisplayName("Translation API should be called only ONCE (second request uses DB cache)")
    fun testDBCache_ApiCalledOnlyOnce() {
        // Given: Service uses DB caching
        val request = TranslationRequestDTO(
            contentType = ContentType.POST,
            contentId = testPost.id,
            targetLang = "ko"
        )

        // Prepare only ONE mock response
        // If API is called twice, the second call will fail (no more enqueued responses)

        // When: Call translation service TWICE
        val firstResult = translationService.getTranslation(request)

        // Force flush to DB and clear JPA 1st level cache
        // This ensures the second call truly reads from DB, not JPA cache
        entityManager.flush()
        entityManager.clear()

        val secondResult = translationService.getTranslation(request)

        // Then: First call should hit API (cached = false)
        assertThat(firstResult.cached).isFalse()
        assertThat(firstResult.translatedText).contains("안녕하세요")

        // Then: Second call should use DB cache (cached = true)
        assertThat(secondResult.cached).isTrue()
        assertThat(secondResult.translatedText).contains("안녕하세요")

        // Verify: MockWebServer received exactly ONE request
        assertThat(Companion.mockWebServer.requestCount).isEqualTo(1)
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    @DisplayName("Same source and target language should return original text (no API call)")
    fun testDBCache_SameLanguageNoApiCall() {
        // Given: Request translation to same language as original
        val request = TranslationRequestDTO(
            contentType = ContentType.POST,
            contentId = testPost.id,
            targetLang = "en"  // Same as originalLang
        )

        // When: Request translation
        val result = translationService.getTranslation(request)

        // Then: Should return original text without calling API
        assertThat(result.translatedText).contains("This is a test post in English")
        assertThat(result.model).isEqualTo("none")
        assertThat(result.cached).isTrue()  // Returned as cached (no API call)

        // Verify: MockWebServer received ZERO requests (no API call needed)
        assertThat(Companion.mockWebServer.requestCount).isEqualTo(0)
    }
}
