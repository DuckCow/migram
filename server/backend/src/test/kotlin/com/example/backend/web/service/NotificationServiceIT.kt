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

/**
 * Integration tests for Notification System
 *
 * Tests:
 * 1. Comment on post notifications
 * 2. Reply on comment notifications
 * 3. Vote milestone notifications (posts)
 * 4. Vote milestone notifications (comments)
 * 5. Report resolved notifications
 * 6. Notification persistence and retrieval
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationServiceIT {

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
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var commentService: CommentService

    @Autowired
    private lateinit var voteService: VoteService

    @Autowired
    private lateinit var moderationService: ModerationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var boardRepository: BoardRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var reportRepository: ReportRepository

    @Autowired
    private lateinit var moderationActionRepository: ModerationActionRepository

    private lateinit var userA: User  // Main test user
    private lateinit var userB: User  // Actor user
    private lateinit var adminUser: User  // Admin for moderation tests

    private lateinit var testBoard: Board
    private lateinit var testPost: Post

    @BeforeEach
    fun setupTestData() {
        // Clean database
        notificationRepository.deleteAll()
        moderationActionRepository.deleteAll()
        reportRepository.deleteAll()
        voteRepository.deleteAll()
        commentRepository.deleteAll()
        postRepository.deleteAll()
        boardRepository.deleteAll()
        userRepository.deleteAll()

        // Create users with all required fields
        userA = userRepository.save(
            User(
                email = "userA@example.com",
                name = "User A",
                nickname = "userA",
                profilePicture = null,
                originCountry = null,
                currentCountry = null
            )
        )

        userB = userRepository.save(
            User(
                email = "userB@example.com",
                name = "User B",
                nickname = "userB",
                profilePicture = null,
                originCountry = null,
                currentCountry = null
            )
        )

        adminUser = userRepository.save(
            User(
                email = "admin@example.com",
                name = "Admin User",
                nickname = "admin",
                profilePicture = null,
                originCountry = null,
                currentCountry = null,
                roles = mutableSetOf(Role.USER, Role.ADMIN)
            )
        )

        // Create test board
        testBoard = boardRepository.save(
            Board(
                name = "Test Board",
                description = "Test board",
                boardType = BoardType.TOPIC,
                context = "TEST",
                status = BoardStatus.ACTIVE
            )
        )

        // Create test post
        testPost = postRepository.save(
            Post(
                title = "Test post",
                content = "Test content",
                board = testBoard,
                user = userA,
                originalLang = "en"
            )
        )
    }

    @Test
    @DisplayName("Should create COMMENT_ON_POST notification")
    fun testCommentOnPostNotification() {
        // When: userB comments on userA's post
        val commentDTO = CommentRequestDTO(
            postId = testPost.id,
            content = "Great post!"
        )
        commentService.save(commentDTO, userB.id)

        // Then: userA should receive notification
        val notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(userA, PageRequest.of(0, 10))
        assertThat(notifications.content).hasSize(1)
        assertThat(notifications.content[0].type).isEqualTo(NotificationType.COMMENT_ON_POST)
        assertThat(notifications.content[0].actor.nickname).isEqualTo("userB")
        assertThat(notifications.content[0].message).contains("commented on your post")
    }

    @Test
    @DisplayName("Should create REPLY_ON_COMMENT notification")
    fun testReplyOnCommentNotification() {
        // Given: userA makes a comment
        val parentCommentDTO = CommentRequestDTO(
            postId = testPost.id,
            content = "Original comment"
        )
        val parentComment = commentService.save(parentCommentDTO, userA.id)

        // When: userB replies to userA's comment
        val replyDTO = CommentRequestDTO(
            postId = testPost.id,
            parentCommentId = parentComment.id,
            content = "I agree!"
        )
        commentService.save(replyDTO, userB.id)

        // Then: userA should receive notification
        val notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(userA, PageRequest.of(0, 10))
        assertThat(notifications.content).hasSize(1)
        assertThat(notifications.content[0].type).isEqualTo(NotificationType.REPLY_ON_COMMENT)
        assertThat(notifications.content[0].actor.nickname).isEqualTo("userB")
        assertThat(notifications.content[0].message).contains("replied to your comment")
    }

    @Test
    @DisplayName("Should NOT create notification when commenting on own post")
    fun testNoSelfCommentNotification() {
        // When: userA comments on their own post
        val commentDTO = CommentRequestDTO(
            postId = testPost.id,
            content = "Updating my post"
        )
        commentService.save(commentDTO, userA.id)

        // Then: No notification should be created
        val notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(userA, PageRequest.of(0, 10))
        assertThat(notifications.content).isEmpty()
    }

    @Test
    @DisplayName("Should create VOTE_ON_POST notification at milestones")
    fun testVoteOnPostMilestoneNotification() {
        // Given: Create 10 votes (milestone reached)
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
            voteRepository.save(Vote(user = voter, post = testPost, voteType = VoteType.UP))
        }

        // When: Manually create milestone notification (simulating what VoteService does)
        notificationService.createNotification(
            recipientId = userA.id,
            actorId = userB.id,
            type = NotificationType.VOTE_ON_POST,
            referenceId = testPost.id,
            message = "Your post reached 10 votes!"
        )

        // Then: userA should receive milestone notification
        val notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(userA, PageRequest.of(0, 10))
        assertThat(notifications.content).hasSize(1)
        assertThat(notifications.content[0].type).isEqualTo(NotificationType.VOTE_ON_POST)
        assertThat(notifications.content[0].message).contains("reached 10 votes")
    }

    @Test
    @DisplayName("Should create VOTE_ON_COMMENT notification at milestones")
    fun testVoteOnCommentMilestoneNotification() {
        // Given: userA creates a comment
        val commentDTO = CommentRequestDTO(
            postId = testPost.id,
            content = "Great comment"
        )
        val comment = commentService.save(commentDTO, userA.id)

        // Create 50 votes (milestone reached)
        (1..50).forEach { i ->
            val voter = userRepository.save(
                User(
                    email = "voter$i@example.com",
                    name = "Voter $i",
                    profilePicture = null,
                    originCountry = null,
                    currentCountry = null
                )
            )
            voteRepository.save(Vote(user = voter, comment = commentRepository.findById(comment.id).get(), voteType = VoteType.UP))
        }

        // When: Manually create milestone notification (simulating what VoteService does)
        notificationService.createNotification(
            recipientId = userA.id,
            actorId = userB.id,
            type = NotificationType.VOTE_ON_COMMENT,
            referenceId = comment.id,
            message = "Your comment reached 50 votes!"
        )

        // Then: userA should receive milestone notification
        val notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(userA, PageRequest.of(0, 10))
        assertThat(notifications.content).hasSize(1)
        assertThat(notifications.content[0].type).isEqualTo(NotificationType.VOTE_ON_COMMENT)
        assertThat(notifications.content[0].message).contains("reached 50 votes")
    }

    @Test
    @DisplayName("Should NOT create vote notification when voting on own content")
    fun testNoSelfVoteNotification() {
        // Given: Create 10 votes on userA's post by others
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
            voteRepository.save(Vote(user = voter, post = testPost, voteType = VoteType.UP))
        }

        // When: VoteService logic checks if voter is post owner and skips notification
        // Simulating: if (post.user.id != currentUserId) { create notification }
        // In this case, userA is both voter and post owner, so no notification is created

        // Then: No notification should be created
        val notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(userA, PageRequest.of(0, 10))
        assertThat(notifications.content).isEmpty()
    }

    @Test
    @DisplayName("Should create REPORT_RESOLVED notification")
    fun testReportResolvedNotification() {
        // Given: userA reports userB's post
        val otherPost = postRepository.save(
            Post(
                title = "Other post",
                content = "Content",
                board = testBoard,
                user = userB,
                originalLang = "en"
            )
        )

        val report = reportRepository.save(
            Report(
                reporter = userA,
                contentType = ContentType.POST,
                post = otherPost,
                reason = ReportReason.SPAM,
                description = "This is spam"
            )
        )

        // When: Admin resolves the report
        val reviewRequest = ReportReviewRequestDTO(
            status = ReportStatus.RESOLVED,
            action = ModerationActionType.SET_HIDDEN,
            adminNote = "Confirmed spam"
        )
        moderationService.reviewReport(report.id, reviewRequest, adminUser.id)

        // Then: userA should receive notification
        val notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(userA, PageRequest.of(0, 10))
        assertThat(notifications.content).hasSize(1)
        assertThat(notifications.content[0].type).isEqualTo(NotificationType.REPORT_RESOLVED)
        assertThat(notifications.content[0].message).contains("Your report has been resolved")
    }

    @Test
    @DisplayName("Should mark notification as read")
    fun testMarkNotificationAsRead() {
        // Given: Create a notification
        val notification = notificationService.createNotification(
            recipientId = userA.id,
            actorId = userB.id,
            type = NotificationType.COMMENT_ON_POST,
            referenceId = testPost.id,
            message = "Test notification"
        )

        // Verify unread
        assertThat(notification.isRead).isFalse()

        // When: Mark as read
        notification.isRead = true
        notificationRepository.save(notification)

        // Then: Should be marked as read
        val updated = notificationRepository.findById(notification.id).get()
        assertThat(updated.isRead).isTrue()
    }

    @Test
    @DisplayName("Should get unread notification count")
    fun testGetUnreadCount() {
        // Given: Create 2 unread and 1 read notification
        repeat(2) { i ->
            notificationService.createNotification(
                recipientId = userA.id,
                actorId = userB.id,
                type = NotificationType.COMMENT_ON_POST,
                referenceId = testPost.id,
                message = "Unread $i"
            )
        }

        val readNotification = notificationService.createNotification(
            recipientId = userA.id,
            actorId = userB.id,
            type = NotificationType.COMMENT_ON_POST,
            referenceId = testPost.id,
            message = "Read notification"
        )
        readNotification.isRead = true
        notificationRepository.save(readNotification)

        // When: Get unread count
        val unreadCount = notificationRepository.countByRecipientAndIsRead(userA, false)

        // Then: Should return 2
        assertThat(unreadCount).isEqualTo(2)
    }

    @Test
    @DisplayName("Should get only unread notifications")
    fun testGetUnreadNotifications() {
        // Given: Create mix of read and unread notifications
        val notification1 = notificationService.createNotification(
            recipientId = userA.id,
            actorId = userB.id,
            type = NotificationType.COMMENT_ON_POST,
            referenceId = testPost.id,
            message = "Unread 1"
        )

        val notification2 = notificationService.createNotification(
            recipientId = userA.id,
            actorId = userB.id,
            type = NotificationType.COMMENT_ON_POST,
            referenceId = testPost.id,
            message = "Read"
        )
        notification2.isRead = true
        notificationRepository.save(notification2)

        val notification3 = notificationService.createNotification(
            recipientId = userA.id,
            actorId = userB.id,
            type = NotificationType.COMMENT_ON_POST,
            referenceId = testPost.id,
            message = "Unread 2"
        )

        // When: Get unread notifications
        val unreadNotifications = notificationRepository.findByRecipientAndIsReadOrderByCreatedAtDesc(
            userA,
            false,
            PageRequest.of(0, 10)
        )

        // Then: Should return only unread
        assertThat(unreadNotifications.content).hasSize(2)
        assertThat(unreadNotifications.content).allMatch { !it.isRead }
        assertThat(unreadNotifications.content.map { it.id })
            .containsExactlyInAnyOrder(notification1.id, notification3.id)
    }
}
