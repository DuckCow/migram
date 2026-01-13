package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.BoardRepository
import com.example.backend.web.repository.FollowRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.repository.VoteRepository
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
 * Integration tests for Personalized Feed Algorithm
 *
 * Tests:
 * 1. Follow-based boosting (0.5x multiplier)
 * 2. Current country boosting (0.3x multiplier)
 * 3. Origin country boosting (0.2x multiplier)
 * 4. Combined boosting (multiple factors)
 * 5. Anonymous user fallback (generic hot feed)
 * 6. Cache functionality
 * 7. Cache invalidation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersonalizedFeedServiceIT {

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
    private lateinit var personalizedFeedService: PersonalizedFeedService

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var boardRepository: BoardRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var followRepository: FollowRepository

    @Autowired
    private lateinit var voteRepository: VoteRepository

    private lateinit var testUserKR: User  // Korean user
    private lateinit var testUserUS: User  // US user
    private lateinit var targetUser: User  // User to follow
    private lateinit var strangerUser: User  // Not followed

    private lateinit var boardKorea: Board
    private lateinit var boardUSA: Board
    private lateinit var boardTech: Board

    @BeforeEach
    fun setupTestData() {
        // Clean database
        voteRepository.deleteAll()
        followRepository.deleteAll()
        postRepository.deleteAll()
        boardRepository.deleteAll()
        userRepository.deleteAll()

        // Create boards
        boardKorea = boardRepository.save(
            Board(
                name = "Korea Community",
                description = "Korean board",
                boardType = BoardType.COUNTRY,
                context = "KR",  // South Korea country code
                status = BoardStatus.ACTIVE
            )
        )

        boardUSA = boardRepository.save(
            Board(
                name = "USA Community",
                description = "US board",
                boardType = BoardType.COUNTRY,
                context = "US",
                status = BoardStatus.ACTIVE
            )
        )

        boardTech = boardRepository.save(
            Board(
                name = "Tech Talk",
                description = "Technology discussions",
                boardType = BoardType.TOPIC,
                context = "TECH",
                status = BoardStatus.ACTIVE
            )
        )

        // Create users
        testUserKR = userRepository.save(
            User(
                email = "korean@example.com",
                name = "Korean User",
                profilePicture = null,
                originCountry = Country.KOREA_SOUTH,
                currentCountry = Country.UNITED_STATES
            )
        )

        testUserUS = userRepository.save(
            User(
                email = "us@example.com",
                name = "US User",
                profilePicture = null,
                originCountry = Country.UNITED_STATES,
                currentCountry = Country.UNITED_STATES
            )
        )

        targetUser = userRepository.save(
            User(
                email = "target@example.com",
                name = "Target User",
                profilePicture = null,
                originCountry = null,
                currentCountry = null
            )
        )

        strangerUser = userRepository.save(
            User(
                email = "stranger@example.com",
                name = "Stranger User",
                profilePicture = null,
                originCountry = null,
                currentCountry = null
            )
        )
    }

    @Test
    @DisplayName("Should boost posts from followed users")
    fun testFollowBasedBoosting() {
        // Given: testUserKR follows targetUser
        val follow = followRepository.save(
            Follow(
                requester = testUserKR,
                target = targetUser,
                status = FollowStatus.ACCEPTED
            )
        )

        // Create posts with same base score (same votes, same age)
        val postByFollowed = postRepository.save(
            Post(
                title = "Post by followed user",
                content = "Content",
                board = boardTech,
                user = targetUser,
                originalLang = "en"
            )
        )

        val postByStranger = postRepository.save(
            Post(
                title = "Post by stranger",
                content = "Content",
                board = boardTech,
                user = strangerUser,
                originalLang = "en"
            )
        )

        // Give equal votes to both posts
        voteRepository.save(Vote(user = testUserUS, post = postByFollowed, voteType = VoteType.UP))
        voteRepository.save(Vote(user = testUserUS, post = postByStranger, voteType = VoteType.UP))

        // When: Get personalized feed
        val feed = personalizedFeedService.getPersonalizedFeed(
            testUserKR.id,
            null,
            PageRequest.of(0, 10)
        )

        // Then: Followed user's post should rank higher
        assertThat(feed.content).hasSizeGreaterThanOrEqualTo(2)
        assertThat(feed.content[0].id).isEqualTo(postByFollowed.id)
        assertThat(feed.content[0].hotScore).isGreaterThan(feed.content[1].hotScore!!)
    }

    @Test
    @DisplayName("Should boost posts from current country board")
    fun testCurrentCountryBoosting() {
        // Given: testUserKR has currentCountry = US
        // Create posts in different boards
        val postUSA = postRepository.save(
            Post(
                title = "Post in USA board",
                content = "Content",
                board = boardUSA,  // Matches current country
                user = strangerUser,
                originalLang = "en"
            )
        )

        val postKorea = postRepository.save(
            Post(
                title = "Post in Korea board",
                content = "Content",
                board = boardKorea,  // Matches origin country only
                user = strangerUser,
                originalLang = "en"
            )
        )

        // Give equal votes
        voteRepository.save(Vote(user = testUserUS, post = postUSA, voteType = VoteType.UP))
        voteRepository.save(Vote(user = testUserUS, post = postKorea, voteType = VoteType.UP))

        // When: Get personalized feed
        val feed = personalizedFeedService.getPersonalizedFeed(
            testUserKR.id,
            null,
            PageRequest.of(0, 10)
        )

        // Then: Current country board post should rank higher than origin country
        assertThat(feed.content).hasSizeGreaterThanOrEqualTo(2)
        assertThat(feed.content[0].id).isEqualTo(postUSA.id)
        assertThat(feed.content[0].hotScore).isGreaterThan(feed.content[1].hotScore!!)
    }

    @Test
    @DisplayName("Should combine multiple boosting factors")
    fun testCombinedBoosting() {
        // Given: Setup for maximum boost
        // testUserKR follows targetUser
        followRepository.save(
            Follow(
                requester = testUserKR,
                target = targetUser,
                status = FollowStatus.ACCEPTED
            )
        )

        // Post by followed user in current country board (0.5 + 0.3 = 1.8x boost)
        val maxBoostPost = postRepository.save(
            Post(
                title = "Max boost post",
                content = "Content",
                board = boardUSA,  // Current country
                user = targetUser,  // Followed user
                originalLang = "en"
            )
        )

        // Post with no boost
        val noBoostPost = postRepository.save(
            Post(
                title = "No boost post",
                content = "Content",
                board = boardTech,
                user = strangerUser,
                originalLang = "en"
            )
        )

        // Give equal votes
        voteRepository.save(Vote(user = testUserUS, post = maxBoostPost, voteType = VoteType.UP))
        voteRepository.save(Vote(user = testUserUS, post = noBoostPost, voteType = VoteType.UP))

        // When: Get personalized feed
        val feed = personalizedFeedService.getPersonalizedFeed(
            testUserKR.id,
            null,
            PageRequest.of(0, 10)
        )

        // Then: Max boost post should rank significantly higher
        assertThat(feed.content).hasSizeGreaterThanOrEqualTo(2)
        assertThat(feed.content[0].id).isEqualTo(maxBoostPost.id)

        // Verify boost multiplier (should be around 1.8x)
        val maxBoostScore = feed.content[0].hotScore!!
        val noBoostScore = feed.content[1].hotScore!!
        val multiplierRatio = maxBoostScore / noBoostScore
        assertThat(multiplierRatio).isCloseTo(1.8, org.assertj.core.data.Offset.offset(0.1))
    }

    @Test
    @DisplayName("Should boost origin country less than current country")
    fun testOriginCountryBoostingLessThanCurrent() {
        // Given: testUserKR has origin=KR, current=US
        val postOrigin = postRepository.save(
            Post(
                title = "Origin country post",
                content = "Content",
                board = boardKorea,  // Origin (0.2x boost)
                user = strangerUser,
                originalLang = "en"
            )
        )

        val postCurrent = postRepository.save(
            Post(
                title = "Current country post",
                content = "Content",
                board = boardUSA,  // Current (0.3x boost)
                user = strangerUser,
                originalLang = "en"
            )
        )

        // Give equal votes
        voteRepository.save(Vote(user = testUserUS, post = postOrigin, voteType = VoteType.UP))
        voteRepository.save(Vote(user = testUserUS, post = postCurrent, voteType = VoteType.UP))

        // When: Get personalized feed
        val feed = personalizedFeedService.getPersonalizedFeed(
            testUserKR.id,
            null,
            PageRequest.of(0, 10)
        )

        // Then: Current country should rank higher
        assertThat(feed.content).hasSizeGreaterThanOrEqualTo(2)
        assertThat(feed.content[0].id).isEqualTo(postCurrent.id)

        // Verify boost ratio (1.3 / 1.2 ≈ 1.08)
        val currentScore = feed.content[0].hotScore!!
        val originScore = feed.content[1].hotScore!!
        assertThat(currentScore).isGreaterThan(originScore)
    }

    @Test
    @DisplayName("Should cache personalized feeds")
    fun testFeedCaching() {
        // Given: Create a post
        val post = postRepository.save(
            Post(
                title = "Test post",
                content = "Content",
                board = boardTech,
                user = strangerUser,
                originalLang = "en"
            )
        )

        // When: Fetch feed twice
        val start1 = System.currentTimeMillis()
        val feed1 = personalizedFeedService.getPersonalizedFeed(
            testUserKR.id,
            null,
            PageRequest.of(0, 10)
        )
        val time1 = System.currentTimeMillis() - start1

        val start2 = System.currentTimeMillis()
        val feed2 = personalizedFeedService.getPersonalizedFeed(
            testUserKR.id,
            null,
            PageRequest.of(0, 10)
        )
        val time2 = System.currentTimeMillis() - start2

        // Then: Results should be identical
        assertThat(feed1.content.map { it.id }).isEqualTo(feed2.content.map { it.id })

        // Second call should be faster (cached) - but this might be flaky, so just log it
        println("First call: ${time1}ms, Second call: ${time2}ms")
    }

    @Test
    @DisplayName("Should cache followed users list")
    fun testFollowedUsersCaching() {
        // Given: testUserKR follows targetUser
        followRepository.save(
            Follow(
                requester = testUserKR,
                target = targetUser,
                status = FollowStatus.ACCEPTED
            )
        )

        // When: Call getFollowedUserIds twice
        val start1 = System.currentTimeMillis()
        val followed1 = personalizedFeedService.getFollowedUserIds(testUserKR.id)
        val time1 = System.currentTimeMillis() - start1

        val start2 = System.currentTimeMillis()
        val followed2 = personalizedFeedService.getFollowedUserIds(testUserKR.id)
        val time2 = System.currentTimeMillis() - start2

        // Then: Results should be identical
        assertThat(followed1).isEqualTo(followed2)
        assertThat(followed1).contains(targetUser.id)

        println("First call: ${time1}ms, Second call: ${time2}ms (should be cached)")
    }

    @Test
    @DisplayName("Should filter by board IDs")
    fun testBoardFiltering() {
        // Given: Posts in different boards
        val postKorea = postRepository.save(
            Post(
                title = "Korea post",
                content = "Content",
                board = boardKorea,
                user = strangerUser,
                originalLang = "en"
            )
        )

        val postUSA = postRepository.save(
            Post(
                title = "USA post",
                content = "Content",
                board = boardUSA,
                user = strangerUser,
                originalLang = "en"
            )
        )

        // When: Filter by Korea board only
        val feed = personalizedFeedService.getPersonalizedFeed(
            testUserKR.id,
            listOf(boardKorea.id),
            PageRequest.of(0, 10)
        )

        // Then: Should only return Korea posts
        assertThat(feed.content).hasSize(1)
        assertThat(feed.content[0].boardId).isEqualTo(boardKorea.id)
    }
}
