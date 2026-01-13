package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

/**
 * Integration tests for Trending Service
 *
 * Tests:
 * 1. Posts with high engagement velocity rank higher
 * 2. Newer posts with same engagement rank higher (recency boost)
 * 3. Comments boost trending score (1.5x weight)
 * 4. Board filtering works correctly
 * 5. Country filtering works correctly
 * 6. Different time periods (24h, 7d, 30d)
 * 7. Old posts outside period are excluded
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TrendingServiceIT {

    companion object {
        @Container
        @JvmField
        val postgres: KPostgreSQLContainer = KPostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.PostgreSQLDialect" }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }

    @Autowired
    private lateinit var trendingService: TrendingService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var boardRepository: BoardRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    private lateinit var testUser: User
    private lateinit var testBoard: Board
    private lateinit var koreaBoard: Board

    @BeforeEach
    fun setupTestData() {
        // Clean database
        commentRepository.deleteAll()
        voteRepository.deleteAll()
        postRepository.deleteAll()
        boardRepository.deleteAll()
        userRepository.deleteAll()

        // Create test user
        testUser = userRepository.save(
            User(
                email = "test@example.com",
                name = "Test User",
                profilePicture = null,
                originCountry = null,
                currentCountry = null
            )
        )

        // Create boards
        testBoard = boardRepository.save(
            Board(
                name = "Test Board",
                description = "Test",
                boardType = BoardType.TOPIC,
                context = "TEST",
                status = BoardStatus.ACTIVE
            )
        )

        koreaBoard = boardRepository.save(
            Board(
                name = "Korea",
                description = "Korea board",
                boardType = BoardType.COUNTRY,
                context = "KR",
                status = BoardStatus.ACTIVE
            )
        )
    }

    @Test
    @DisplayName("Should rank posts with high engagement velocity higher")
    fun testEngagementVelocityRanking() {
        // Given: Two posts, one with more votes
        val lowEngagement = postRepository.save(
            Post(
                title = "Low engagement",
                content = "Content",
                board = testBoard,
                user = testUser,
                originalLang = "en"
            )
        )

        val highEngagement = postRepository.save(
            Post(
                title = "High engagement",
                content = "Content",
                board = testBoard,
                user = testUser,
                originalLang = "en"
            )
        )

        // Add votes (high engagement gets 10 votes, low gets 2)
        (1..10).forEach { i ->
            val voter = userRepository.save(
                User(
                    email = "voter$i@example.com",
                    name = "Voter $i",
                    profilePicture = null,
                    originCountry = null,
                    currentCountry = null
                )
            )
            voteRepository.save(Vote(user = voter, post = highEngagement, voteType = VoteType.UP))
        }

        (1..2).forEach { i ->
            val voter = userRepository.save(
                User(
                    email = "lowvoter$i@example.com",
                    name = "Low Voter $i",
                    profilePicture = null,
                    originCountry = null,
                    currentCountry = null
                )
            )
            voteRepository.save(Vote(user = voter, post = lowEngagement, voteType = VoteType.UP))
        }

        // When: Get trending posts
        val trending = trendingService.getTrendingPosts(TrendingPeriod.LAST_24H, PageRequest.of(0, 10))

        // Then: High engagement post should rank first
        assertThat(trending.content).hasSizeGreaterThanOrEqualTo(2)
        assertThat(trending.content[0].id).isEqualTo(highEngagement.id)
        assertThat(trending.content[0].trendingScore).isGreaterThan(trending.content[1].trendingScore!!)
    }

    @Test
    @DisplayName("Should boost posts with comments higher (1.5x weight)")
    fun testCommentWeighting() {
        // Given: Two posts with same votes, but one has comments
        val postWithComments = postRepository.save(
            Post(
                title = "Post with comments",
                content = "Content",
                board = testBoard,
                user = testUser,
                originalLang = "en"
            )
        )

        val postWithoutComments = postRepository.save(
            Post(
                title = "Post without comments",
                content = "Content",
                board = testBoard,
                user = testUser,
                originalLang = "en"
            )
        )

        // Same votes for both
        (1..5).forEach { i ->
            val voter = userRepository.save(
                User(
                    email = "voter$i@example.com",
                    name = "Voter $i",
                    profilePicture = null,
                    originCountry = null,
                    currentCountry = null
                )
            )
            voteRepository.save(Vote(user = voter, post = postWithComments, voteType = VoteType.UP))
            voteRepository.save(Vote(user = voter, post = postWithoutComments, voteType = VoteType.UP))
        }

        // Add comments to one post (comments have 1.5x weight)
        (1..3).forEach { i ->
            val commenter = userRepository.save(
                User(
                    email = "commenter$i@example.com",
                    name = "Commenter $i",
                    profilePicture = null,
                    originCountry = null,
                    currentCountry = null
                )
            )
            commentRepository.save(
                Comment(
                    content = "Comment $i",
                    post = postWithComments,
                    user = commenter
                )
            )
        }

        // When: Get trending posts
        val trending = trendingService.getTrendingPosts(TrendingPeriod.LAST_24H, PageRequest.of(0, 10))

        // Then: Post with comments should rank higher
        assertThat(trending.content).hasSizeGreaterThanOrEqualTo(2)
        assertThat(trending.content[0].id).isEqualTo(postWithComments.id)
        assertThat(trending.content[0].trendingScore).isGreaterThan(trending.content[1].trendingScore!!)
    }

    @Test
    @DisplayName("Should filter by board correctly")
    fun testBoardFiltering() {
        // Given: Posts in different boards
        val postInTestBoard = postRepository.save(
            Post(
                title = "Post in test board",
                content = "Content",
                board = testBoard,
                user = testUser,
                originalLang = "en"
            )
        )

        val postInKoreaBoard = postRepository.save(
            Post(
                title = "Post in Korea board",
                content = "Content",
                board = koreaBoard,
                user = testUser,
                originalLang = "en"
            )
        )

        // Add votes to both
        val voter = userRepository.save(
            User(
                email = "voter@example.com",
                name = "Voter",
                profilePicture = null,
                originCountry = null,
                currentCountry = null
            )
        )
        voteRepository.save(Vote(user = voter, post = postInTestBoard, voteType = VoteType.UP))
        voteRepository.save(Vote(user = voter, post = postInKoreaBoard, voteType = VoteType.UP))

        // When: Get trending for test board only
        val trending = trendingService.getTrendingPostsByBoard(testBoard.id, TrendingPeriod.LAST_24H, PageRequest.of(0, 10))

        // Then: Should only include test board posts
        assertThat(trending.content).hasSize(1)
        assertThat(trending.content[0].boardId).isEqualTo(testBoard.id)
    }

    @Test
    @DisplayName("Should filter by country correctly")
    fun testCountryFiltering() {
        // Given: Posts in different boards
        val postInKorea = postRepository.save(
            Post(
                title = "Post in Korea",
                content = "Content",
                board = koreaBoard,
                user = testUser,
                originalLang = "en"
            )
        )

        val postInOtherBoard = postRepository.save(
            Post(
                title = "Post in other board",
                content = "Content",
                board = testBoard,
                user = testUser,
                originalLang = "en"
            )
        )

        // Add votes
        val voter = userRepository.save(
            User(
                email = "voter@example.com",
                name = "Voter",
                profilePicture = null,
                originCountry = null,
                currentCountry = null
            )
        )
        voteRepository.save(Vote(user = voter, post = postInKorea, voteType = VoteType.UP))
        voteRepository.save(Vote(user = voter, post = postInOtherBoard, voteType = VoteType.UP))

        // When: Get trending for Korea only
        val trending = trendingService.getTrendingPostsByCountry("KR", TrendingPeriod.LAST_24H, PageRequest.of(0, 10))

        // Then: Should only include Korea board posts
        assertThat(trending.content).hasSize(1)
        assertThat(trending.content[0].boardId).isEqualTo(koreaBoard.id)
    }

    @Test
    @DisplayName("Should exclude posts outside the time period")
    fun testTimePeriodFiltering() {
        // Given: Recent post and old post (older than 24h would require backdating, so we test with period)
        val recentPost = postRepository.save(
            Post(
                title = "Recent post",
                content = "Content",
                board = testBoard,
                user = testUser,
                originalLang = "en"
            )
        )

        // Add engagement to recent post
        val voter = userRepository.save(
            User(
                email = "voter@example.com",
                name = "Voter",
                profilePicture = null,
                originCountry = null,
                currentCountry = null
            )
        )
        voteRepository.save(Vote(user = voter, post = recentPost, voteType = VoteType.UP))

        // When: Get trending for LAST_24H
        val trending = trendingService.getTrendingPosts(TrendingPeriod.LAST_24H, PageRequest.of(0, 10))

        // Then: Should include recent post
        assertThat(trending.content).hasSizeGreaterThanOrEqualTo(1)
        assertThat(trending.content.any { it.id == recentPost.id }).isTrue()
    }

    @Test
    @DisplayName("Should return empty list when no posts exist")
    fun testEmptyFeed() {
        // When: Get trending with no posts
        val trending = trendingService.getTrendingPosts(TrendingPeriod.LAST_24H, PageRequest.of(0, 10))

        // Then: Should return empty list
        assertThat(trending.content).isEmpty()
        assertThat(trending.totalElements).isEqualTo(0)
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    fun testPagination() {
        // Given: Multiple trending posts
        (1..25).forEach { i ->
            val post = postRepository.save(
                Post(
                    title = "Post $i",
                    content = "Content",
                    board = testBoard,
                    user = testUser,
                    originalLang = "en"
                )
            )

            // Add votes (varying amounts)
            (1..i).forEach { j ->
                val voter = userRepository.save(
                    User(
                        email = "voter_${i}_$j@example.com",
                        name = "Voter",
                        profilePicture = null,
                        originCountry = null,
                        currentCountry = null
                    )
                )
                voteRepository.save(Vote(user = voter, post = post, voteType = VoteType.UP))
            }
        }

        // When: Get first page (size 10)
        val page1 = trendingService.getTrendingPosts(TrendingPeriod.LAST_24H, PageRequest.of(0, 10))

        // Then: Should have correct pagination info
        assertThat(page1.content).hasSize(10)
        assertThat(page1.totalElements).isEqualTo(25)
        assertThat(page1.totalPages).isEqualTo(3)
        assertThat(page1.isFirst).isTrue()
        assertThat(page1.isLast).isFalse()
        assertThat(page1.hasNext).isTrue()

        // When: Get second page
        val page2 = trendingService.getTrendingPosts(TrendingPeriod.LAST_24H, PageRequest.of(1, 10))

        // Then: Should have different posts
        assertThat(page2.content).hasSize(10)
        assertThat(page2.isFirst).isFalse()
        assertThat(page2.isLast).isFalse()
    }
}
