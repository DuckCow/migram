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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Integration test for TranslationService Redis fallback behavior
 *
 * Objective: Verify that when Redis is unavailable, the service gracefully degrades to DB-only caching
 *
 * Test scenarios:
 * 1. Redis down (misconfigured host) → Service works via DB fallback
 * 2. Translation saved to DB cache when Redis unavailable
 * 3. Translation API called only once (second request uses DB cache)
 * 4. Same source/target language returns original (no API call)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TranslationRedisFallbackIT {

    companion object {
        private val mockWebServer = MockWebServer()

        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @BeforeAll
        fun startMockServer() {
            mockWebServer.start()
        }

        @JvmStatic
        @AfterAll
        fun stopMockServer() {
            mockWebServer.shutdown()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // PostgreSQL configuration
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }

            // Redis configuration: Force Redis cache type but with WRONG host to make it unavailable
            // This simulates a production scenario where Redis is configured but unreachable
            registry.add("spring.cache.type") { "redis" }
            registry.add("spring.data.redis.host") { "invalid-redis-host-does-not-exist" }
            registry.add("spring.data.redis.port") { "6390" }  // Wrong port

            // Translation service configuration: Point to mock server
            registry.add("translation.base-url") { mockWebServer.url("/").toString().trimEnd('/') }
            registry.add("translation.timeout-ms") { "5000" }

            // Flyway disabled for test (using ddl-auto)
            registry.add("spring.flyway.enabled") { "false" }
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

    private lateinit var testPost: Post

    @BeforeEach
    fun setupTestData() {
        // Clear database before each test
        contentTranslationRepository.deleteAll()
        postRepository.deleteAll()
        userRepository.deleteAll()
        boardRepository.deleteAll()

        // Clear any remaining mock responses from previous tests
        while (Companion.mockWebServer.requestCount > 0) {
            Companion.mockWebServer.takeRequest()
        }

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

        // Prepare mock response for translation API
        val mockResponse = MockResponse()
            .setBody("""{"translatedText": "안녕하세요 세계\n\n이것은 영어로 된 테스트 게시물입니다.", "model": "opus-mt-en-ko"}""")
            .addHeader("Content-Type", "application/json")
        Companion.mockWebServer.enqueue(mockResponse)
    }

    @Test
    @DisplayName("Redis down → Service should fallback to DB cache and work without errors")
    fun testRedisFallback_ServiceWorksWithoutErrors() {
        // Given: Redis is unavailable (configured with wrong host in DynamicPropertySource)
        // When: Request translation for the first time
        val request = TranslationRequestDTO(
            contentType = ContentType.POST,
            contentId = testPost.id,
            targetLang = "ko"
        )

        // Then: Should NOT throw 500 error despite Redis being down
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
    @DisplayName("Translation should be saved to DB cache when Redis is unavailable")
    fun testRedisFallback_TranslationSavedToDatabase() {
        // Given: Redis is unavailable
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
    @DisplayName("Translation API should be called only ONCE (second request uses DB cache)")
    fun testRedisFallback_ApiCalledOnlyOnce() {
        // Given: Redis is unavailable
        val request = TranslationRequestDTO(
            contentType = ContentType.POST,
            contentId = testPost.id,
            targetLang = "ko"
        )

        // Prepare only ONE mock response
        // If API is called twice, the second call will fail (no more enqueued responses)

        // When: Call translation service TWICE
        val firstResult = translationService.getTranslation(request)
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
    @DisplayName("Same source and target language should return original text (no API call)")
    fun testRedisFallback_SameLanguageNoApiCall() {
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
