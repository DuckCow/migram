package com.example.backend

import com.example.backend.web.model.*
import com.example.backend.web.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDateTime

@Configuration
@Profile("dev")  // Only run in development
class DataLoader {

    @Bean
    fun loadTestData(
        userRepository: UserRepository,
        postRepository: PostRepository,
        boardRepository: BoardRepository,
        commentRepository: CommentRepository,
        voteRepository: VoteRepository,
        followRepository: FollowRepository,
        chatRoomRepository: ChatRoomRepository,
        messageRepository: MessageRepository
    ) = CommandLineRunner {
        println("=== Loading test data ===")

        // Check if basic test data already exists
        if (userRepository.count() > 0) {
            println("Test data already exists, checking for actual user chat data...")

            // Add chat data for actual user if exists (dlerghzld@gmail.com - Wild Sparrow)
            val actualUser = userRepository.findByEmail("dlerghzld@gmail.com")
            if (actualUser != null) {
                println("Found actual user: ${actualUser.nickname} (${actualUser.email})")

                // Get test users
                val user1 = userRepository.findByEmail("test1@example.com")
                    ?: run { println("Test user 1 not found"); return@CommandLineRunner }
                val user2 = userRepository.findByEmail("test2@example.com")
                    ?: run { println("Test user 2 not found"); return@CommandLineRunner }

                val now = LocalDateTime.now()

                // Create mutual follows between actual user and test users
                val followsToProcess = mutableListOf<Follow>()

                // Check and update/create follow from actualUser to user1
                val follow1 = followRepository.findByRequesterAndTarget(actualUser, user1)
                if (follow1 == null) {
                    followsToProcess.add(Follow(
                        requester = actualUser,
                        target = user1,
                        status = FollowStatus.ACCEPTED,
                        respondedAt = now.minusDays(2)
                    ))
                } else if (follow1.status != FollowStatus.ACCEPTED) {
                    follow1.status = FollowStatus.ACCEPTED
                    follow1.respondedAt = now.minusDays(2)
                    followsToProcess.add(follow1)
                }

                val follow2 = followRepository.findByRequesterAndTarget(user1, actualUser)
                if (follow2 == null) {
                    followsToProcess.add(Follow(
                        requester = user1,
                        target = actualUser,
                        status = FollowStatus.ACCEPTED,
                        respondedAt = now.minusDays(2)
                    ))
                } else if (follow2.status != FollowStatus.ACCEPTED) {
                    follow2.status = FollowStatus.ACCEPTED
                    follow2.respondedAt = now.minusDays(2)
                    followsToProcess.add(follow2)
                }

                // Check and update/create follow from actualUser to user2
                val follow3 = followRepository.findByRequesterAndTarget(actualUser, user2)
                if (follow3 == null) {
                    followsToProcess.add(Follow(
                        requester = actualUser,
                        target = user2,
                        status = FollowStatus.ACCEPTED,
                        respondedAt = now.minusDays(1)
                    ))
                } else if (follow3.status != FollowStatus.ACCEPTED) {
                    follow3.status = FollowStatus.ACCEPTED
                    follow3.respondedAt = now.minusDays(1)
                    followsToProcess.add(follow3)
                }

                val follow4 = followRepository.findByRequesterAndTarget(user2, actualUser)
                if (follow4 == null) {
                    followsToProcess.add(Follow(
                        requester = user2,
                        target = actualUser,
                        status = FollowStatus.ACCEPTED,
                        respondedAt = now.minusDays(1)
                    ))
                } else if (follow4.status != FollowStatus.ACCEPTED) {
                    follow4.status = FollowStatus.ACCEPTED
                    follow4.respondedAt = now.minusDays(1)
                    followsToProcess.add(follow4)
                }

                if (followsToProcess.isNotEmpty()) {
                    followRepository.saveAll(followsToProcess)
                    println("Processed ${followsToProcess.size} mutual follows for ${actualUser.nickname}")
                } else {
                    println("All follow relationships already ACCEPTED for ${actualUser.nickname}")
                }

                // Check if chat data already exists for this user
                val existingChatRooms = chatRoomRepository.findAll().filter {
                    it.containsUser(actualUser.id)
                }

                if (existingChatRooms.isNotEmpty()) {
                    println("Chat data already exists for ${actualUser.nickname}, skipping chat creation...")
                    return@CommandLineRunner
                }

                // Create chat room with user1 (Fast Cat)
                // First message sent by user1, so user1 is the initiator
                val chatWithUser1 = ChatRoom(
                    user1 = actualUser,
                    user2 = user1,
                    initiator = user1,
                    status = ChatRoomStatus.ACTIVE,
                    lastMessageAt = now.minusMinutes(30),
                    lastMessagePreview = "How about 3pm at the usual cafe?"
                )

                // Create chat room with user2 (Slow Rabbit)
                // First message sent by actualUser, so actualUser is the initiator
                val chatWithUser2 = ChatRoom(
                    user1 = actualUser,
                    user2 = user2,
                    initiator = actualUser,
                    status = ChatRoomStatus.ACTIVE,
                    lastMessageAt = now.minusHours(1),
                    lastMessagePreview = "Stanley Park and Grouse Mountain are must-sees!"
                )

                chatRoomRepository.saveAll(listOf(chatWithUser1, chatWithUser2))
                println("Created 2 chat rooms for ${actualUser.nickname}")

                // Add messages for actual user
                val actualUserMessages = listOf(
                    // Chat with Fast Cat (user1)
                    Message(
                        chatRoom = chatWithUser1,
                        sender = user1,
                        content = "Hey ${actualUser.nickname}! How are you doing?",
                        isRead = true,
                        readAt = now.minusHours(2),
                        createdAt = now.minusHours(3)
                    ),
                    Message(
                        chatRoom = chatWithUser1,
                        sender = actualUser,
                        content = "Hi Fast Cat! I'm doing great, thanks! How about you?",
                        isRead = true,
                        readAt = now.minusHours(1).minusMinutes(30),
                        createdAt = now.minusHours(2).minusMinutes(30)
                    ),
                    Message(
                        chatRoom = chatWithUser1,
                        sender = user1,
                        content = "All good! Want to meet up for coffee tomorrow?",
                        isRead = true,
                        readAt = now.minusHours(1),
                        createdAt = now.minusHours(1).minusMinutes(15)
                    ),
                    Message(
                        chatRoom = chatWithUser1,
                        sender = actualUser,
                        content = "Sure! What time works for you?",
                        isRead = true,
                        readAt = now.minusMinutes(45),
                        createdAt = now.minusMinutes(50)
                    ),
                    Message(
                        chatRoom = chatWithUser1,
                        sender = user1,
                        content = "How about 3pm at the usual cafe?",
                        isRead = false,
                        createdAt = now.minusMinutes(30)
                    ),

                    // Chat with Slow Rabbit (user2)
                    Message(
                        chatRoom = chatWithUser2,
                        sender = actualUser,
                        content = "Hi! I heard you're from Canada. I've always wanted to visit Vancouver!",
                        isRead = true,
                        readAt = now.minusHours(2),
                        createdAt = now.minusHours(4)
                    ),
                    Message(
                        chatRoom = chatWithUser2,
                        sender = user2,
                        content = "Oh nice! You should definitely come visit. The nature here is amazing!",
                        isRead = true,
                        readAt = now.minusHours(1).minusMinutes(40),
                        createdAt = now.minusHours(2).minusMinutes(30)
                    ),
                    Message(
                        chatRoom = chatWithUser2,
                        sender = actualUser,
                        content = "I'd love to! Any recommendations for places to visit?",
                        isRead = true,
                        readAt = now.minusHours(1).minusMinutes(20),
                        createdAt = now.minusHours(1).minusMinutes(30)
                    ),
                    Message(
                        chatRoom = chatWithUser2,
                        sender = user2,
                        content = "Stanley Park and Grouse Mountain are must-sees! I can show you around if you come.",
                        isRead = false,
                        createdAt = now.minusHours(1)
                    )
                )

                messageRepository.saveAll(actualUserMessages)
                println("Added ${actualUserMessages.size} messages for ${actualUser.nickname}")

                println("=== Chat data added for actual user ===")
            } else {
                println("Actual user (dlerghzld@gmail.com) not found, skipping personalized chat data")
            }

            return@CommandLineRunner
        }

        // Create test users with proper nickname format
        val user1 = User(
            name = "Test User 1",
            email = "test1@example.com",
            profilePicture = null,
            nickname = "Fast Cat",
            nicknameKey = "fast_cat",
            originCountry = Country.JAPAN,
            currentCountry = Country.JAPAN,
            roles = mutableSetOf(Role.USER)
        )

        val user2 = User(
            name = "Test User 2",
            email = "test2@example.com",
            profilePicture = null,
            nickname = "Slow Rabbit",
            nicknameKey = "slow_rabbit",
            originCountry = Country.CANADA,
            currentCountry = Country.CANADA,
            roles = mutableSetOf(Role.USER)
        )

        val user3 = User(
            name = "Test User 3",
            email = "test3@example.com",
            profilePicture = null,
            nickname = "Happy Panda",
            nicknameKey = "happy_panda",
            originCountry = Country.KOREA_SOUTH,
            currentCountry = Country.KOREA_SOUTH,
            roles = mutableSetOf(Role.USER)
        )

        // User with EVERYONE privacy setting (anyone can DM)
        val user4 = User(
            name = "Test User 4",
            email = "test4@example.com",
            profilePicture = null,
            nickname = "Open Fox",
            nicknameKey = "open_fox",
            originCountry = Country.UNITED_STATES,
            currentCountry = Country.UNITED_STATES,
            allowDmsFrom = DmPrivacySetting.EVERYONE,
            roles = mutableSetOf(Role.USER)
        )

        userRepository.saveAll(listOf(user1, user2, user3, user4))
        println("Saved 4 test users (user4 has EVERYONE DM privacy)")

        // Fetch boards for test posts
        val japanBoard = boardRepository.findByContextAndStatus("JAPAN", BoardStatus.ACTIVE)
            ?: throw IllegalStateException("Japan board not found")
        val canadaBoard = boardRepository.findByContextAndStatus("CANADA", BoardStatus.ACTIVE)
            ?: throw IllegalStateException("Canada board not found")
        val koreaBoard = boardRepository.findByContextAndStatus("KOREA_SOUTH", BoardStatus.ACTIVE)
            ?: throw IllegalStateException("Korea board not found")
        val usBoard = boardRepository.findByContextAndStatus("UNITED_STATES", BoardStatus.ACTIVE)
            ?: throw IllegalStateException("US board not found")

        // Create test posts
        val now = LocalDateTime.now()
        val post1 = Post(
            title = "Tokyo Life",
            content = "Life in Tokyo is amazing!",
            mediaUrl = null,
            board = japanBoard,
            user = user1
        ).apply {
            createdAt = now.minusHours(2)
            modifiedAt = now.minusHours(2)
            createdBy = user1.email
        }

        val post2 = Post(
            title = "Vancouver Views",
            content = "Beautiful Vancouver scenery",
            mediaUrl = null,
            board = canadaBoard,
            user = user2
        ).apply {
            createdAt = now.minusHours(5)
            modifiedAt = now.minusHours(5)
            createdBy = user2.email
        }

        val post3 = Post(
            title = "Seoul Food",
            content = "Korean BBQ experience",
            mediaUrl = null,
            board = koreaBoard,
            user = user3
        ).apply {
            createdAt = now.minusHours(1)
            modifiedAt = now.minusHours(1)
            createdBy = user3.email
        }

        val post4 = Post(
            title = "Mount Fuji Trip",
            content = "Visited Mount Fuji yesterday",
            mediaUrl = null,
            board = japanBoard,
            user = user1
        ).apply {
            createdAt = now.minusHours(10)
            modifiedAt = now.minusHours(10)
            createdBy = user1.email
        }

        val post5 = Post(
            title = "Ottawa Winter",
            content = "Cold but beautiful winter in Canada",
            mediaUrl = null,
            board = canadaBoard,
            user = user2
        ).apply {
            createdAt = now.minusDays(1)
            modifiedAt = now.minusDays(1)
            createdBy = user2.email
        }

        val post6 = Post(
            title = "Silicon Valley Tech",
            content = "Innovation happening every day in the Bay Area!",
            mediaUrl = null,
            board = usBoard,
            user = user4
        ).apply {
            createdAt = now.minusHours(6)
            modifiedAt = now.minusHours(6)
            createdBy = user4.email
        }

        postRepository.saveAll(listOf(post1, post2, post3, post4, post5, post6))
        println("Saved 6 test posts")

        // Create test comments
        val comment1 = Comment(
            content = "Great post!",
            post = post1,
            user = user2
        ).apply {
            createdAt = now.minusHours(1)
            modifiedAt = now.minusHours(1)
            createdBy = user2.email
        }

        val comment2 = Comment(
            content = "Thanks for sharing!",
            post = post1,
            user = user3
        ).apply {
            createdAt = now.minusHours(3)
            modifiedAt = now.minusHours(3)
            createdBy = user3.email
        }

        val comment3 = Comment(
            content = "Amazing views!",
            post = post2,
            user = user1
        ).apply {
            createdAt = now.minusHours(4)
            modifiedAt = now.minusHours(4)
            createdBy = user1.email
        }

        commentRepository.saveAll(listOf(comment1, comment2, comment3))
        println("Saved 3 test comments")

        // Create test votes for posts
        val votes = listOf(
            Vote(user = user2, post = post1, voteType = VoteType.UP, createdAt = now.minusHours(1)),
            Vote(user = user3, post = post1, voteType = VoteType.UP, createdAt = now.minusHours(1)),
            Vote(user = user1, post = post2, voteType = VoteType.UP, createdAt = now.minusHours(4)),
            Vote(user = user3, post = post2, voteType = VoteType.UP, createdAt = now.minusHours(4)),
            Vote(user = user1, post = post3, voteType = VoteType.UP, createdAt = now.minusMinutes(30)),
            Vote(user = user2, post = post3, voteType = VoteType.UP, createdAt = now.minusMinutes(30)),
            Vote(user = user2, post = post4, voteType = VoteType.UP, createdAt = now.minusHours(9)),
            Vote(user = user3, post = post4, voteType = VoteType.UP, createdAt = now.minusHours(9)),
            Vote(user = user1, post = post5, voteType = VoteType.UP, createdAt = now.minusDays(1)),
            Vote(user = user1, post = post6, voteType = VoteType.UP, createdAt = now.minusHours(5)),
            Vote(user = user2, post = post6, voteType = VoteType.UP, createdAt = now.minusHours(5)),
            Vote(user = user3, post = post6, voteType = VoteType.UP, createdAt = now.minusHours(5)),
            // Votes for comments
            Vote(user = user1, comment = comment1, voteType = VoteType.UP, createdAt = now.minusMinutes(30)),
            Vote(user = user3, comment = comment1, voteType = VoteType.UP, createdAt = now.minusMinutes(30)),
            Vote(user = user2, comment = comment2, voteType = VoteType.UP, createdAt = now.minusMinutes(30))
        )

        voteRepository.saveAll(votes)
        println("Saved ${votes.size} test votes")

        // Create mutual follow relationships (required for chat with FOLLOWERS_ONLY users)
        // Note: user4 has EVERYONE privacy, so they can receive DMs from anyone (no follow required)
        val follow1to2 = Follow(
            requester = user1,
            target = user2,
            status = FollowStatus.ACCEPTED,
            respondedAt = now.minusDays(5)
        )
        val follow2to1 = Follow(
            requester = user2,
            target = user1,
            status = FollowStatus.ACCEPTED,
            respondedAt = now.minusDays(5)
        )
        val follow1to3 = Follow(
            requester = user1,
            target = user3,
            status = FollowStatus.ACCEPTED,
            respondedAt = now.minusDays(3)
        )
        val follow3to1 = Follow(
            requester = user3,
            target = user1,
            status = FollowStatus.ACCEPTED,
            respondedAt = now.minusDays(3)
        )
        // user1 follows user4 (but user4 doesn't follow back)
        // This tests: user1 can DM user4 (because user4 allows EVERYONE)
        //             user4 CANNOT initiate DM to user1 (because user1 is FOLLOWERS_ONLY and user4 doesn't follow)
        val follow1to4 = Follow(
            requester = user1,
            target = user4,
            status = FollowStatus.ACCEPTED,
            respondedAt = now.minusDays(2)
        )

        followRepository.saveAll(listOf(follow1to2, follow2to1, follow1to3, follow3to1, follow1to4))
        println("Saved 5 follow relationships (user4 has EVERYONE privacy)")

        // Create chat rooms
        // First message in chatRoom1 sent by user1, so user1 is the initiator
        val chatRoom1 = ChatRoom(
            user1 = user1,
            user2 = user2,
            initiator = user1,
            status = ChatRoomStatus.ACTIVE,
            lastMessageAt = now.minusHours(2),
            lastMessagePreview = "That sounds great!"
        )
        // First message in chatRoom2 sent by user3, so user3 is the initiator
        val chatRoom2 = ChatRoom(
            user1 = user1,
            user2 = user3,
            initiator = user3,
            status = ChatRoomStatus.ACTIVE,
            lastMessageAt = now.minusHours(5),
            lastMessagePreview = "Thanks for the recommendation!"
        )

        chatRoomRepository.saveAll(listOf(chatRoom1, chatRoom2))
        println("Saved 2 chat rooms")

        // Create chat messages (bidirectional conversations)
        val messages = listOf(
            // Conversation between user1 and user2
            Message(
                chatRoom = chatRoom1,
                sender = user1,
                content = "Hey! How's the weather in Vancouver?",
                isRead = true,
                readAt = now.minusHours(3),
                createdAt = now.minusHours(4)
            ),
            Message(
                chatRoom = chatRoom1,
                sender = user2,
                content = "It's beautiful! A bit rainy though. How about Tokyo?",
                isRead = true,
                readAt = now.minusHours(2).minusMinutes(30),
                createdAt = now.minusHours(3).minusMinutes(30)
            ),
            Message(
                chatRoom = chatRoom1,
                sender = user1,
                content = "Perfect weather here! Want to grab coffee sometime?",
                isRead = true,
                readAt = now.minusHours(2).minusMinutes(10),
                createdAt = now.minusHours(2).minusMinutes(20)
            ),
            Message(
                chatRoom = chatRoom1,
                sender = user2,
                content = "That sounds great! Let me know when you're free.",
                isRead = false,
                createdAt = now.minusHours(2)
            ),

            // Conversation between user1 and user3
            Message(
                chatRoom = chatRoom2,
                sender = user3,
                content = "Hi! I heard you went to Mount Fuji? How was it?",
                isRead = true,
                readAt = now.minusHours(6),
                createdAt = now.minusHours(8)
            ),
            Message(
                chatRoom = chatRoom2,
                sender = user1,
                content = "Amazing! The view was incredible. Have you been there?",
                isRead = true,
                readAt = now.minusHours(5).minusMinutes(30),
                createdAt = now.minusHours(6).minusMinutes(30)
            ),
            Message(
                chatRoom = chatRoom2,
                sender = user3,
                content = "Not yet, but it's on my bucket list. Any tips?",
                isRead = true,
                readAt = now.minusHours(5).minusMinutes(10),
                createdAt = now.minusHours(5).minusMinutes(20)
            ),
            Message(
                chatRoom = chatRoom2,
                sender = user1,
                content = "Go early morning for the best views! And bring warm clothes.",
                isRead = true,
                readAt = now.minusHours(5),
                createdAt = now.minusHours(5).minusMinutes(5)
            ),
            Message(
                chatRoom = chatRoom2,
                sender = user3,
                content = "Thanks for the recommendation! I'll definitely do that.",
                isRead = false,
                createdAt = now.minusHours(5)
            )
        )

        messageRepository.saveAll(messages)
        println("Saved ${messages.size} chat messages")

        println("=== Test data loading complete ===")
    }
}
