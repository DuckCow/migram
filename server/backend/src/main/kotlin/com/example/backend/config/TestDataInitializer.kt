package com.example.backend.config

import com.example.backend.web.model.DmPrivacySetting
import com.example.backend.web.model.User
import com.example.backend.web.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("dev", "default")
class TestDataInitializer {

    private val logger = LoggerFactory.getLogger(TestDataInitializer::class.java)

    @Bean
    fun initTestData(userRepository: UserRepository) = CommandLineRunner {
        if (userRepository.count() == 0L) {
            logger.info("Creating test users...")

            val user1 = User(
                name = "Test User 1",
                email = "test1@example.com",
                nickname = "TestUser1",
                profilePicture = null,
                originCountry = null,
                currentCountry = null,
                allowDmsFrom = DmPrivacySetting.EVERYONE
            )
            userRepository.save(user1)

            val user2 = User(
                name = "Test User 2",
                email = "test2@example.com",
                nickname = "TestUser2",
                profilePicture = null,
                originCountry = null,
                currentCountry = null,
                allowDmsFrom = DmPrivacySetting.EVERYONE
            )
            userRepository.save(user2)

            logger.info("Test users created: ${user1.email}, ${user2.email}")
        } else {
            logger.info("Database already has users, skipping test data initialization")
        }
    }
}
