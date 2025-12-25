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
        commentRepository: CommentRepository,
        voteRepository: VoteRepository
    ) = CommandLineRunner {
        println("=== Loading test data ===")

        // Check if data already exists
        if (userRepository.count() > 0) {
            println("Test data already exists, skipping...")
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

        userRepository.saveAll(listOf(user1, user2, user3))
        println("Saved 3 test users")

        // Create test posts
        val now = LocalDateTime.now()
        val post1 = Post(
            title = "Tokyo Life",
            content = "Life in Tokyo is amazing!",
            mediaUrl = null,
            country = Country.JAPAN,
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
            country = Country.CANADA,
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
            country = Country.KOREA_SOUTH,
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
            country = Country.JAPAN,
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
            country = Country.CANADA,
            user = user2
        ).apply {
            createdAt = now.minusDays(1)
            modifiedAt = now.minusDays(1)
            createdBy = user2.email
        }

        postRepository.saveAll(listOf(post1, post2, post3, post4, post5))
        println("Saved 5 test posts")

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
            // Votes for comments
            Vote(user = user1, comment = comment1, voteType = VoteType.UP, createdAt = now.minusMinutes(30)),
            Vote(user = user3, comment = comment1, voteType = VoteType.UP, createdAt = now.minusMinutes(30)),
            Vote(user = user2, comment = comment2, voteType = VoteType.UP, createdAt = now.minusMinutes(30))
        )

        voteRepository.saveAll(votes)
        println("Saved ${votes.size} test votes")

        println("=== Test data loading complete ===")
    }
}
