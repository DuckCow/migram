package com.example.backend.web.service

import com.example.backend.web.repository.UserRepository
import org.springframework.stereotype.Service
import kotlin.random.Random

data class NicknameResult(
    val nickname: String,      // Display name with space (e.g., "Fast Cat")
    val nicknameKey: String    // Unique key with underscore (e.g., "fast_cat" or "fast_cat_1234")
)

@Service
class NicknameGeneratorService {
    private val adjectives = listOf(
        "Fast", "Slow", "Happy", "Sad", "Cute", "Brave", "Smart", "Kind",
        "Active", "Quiet", "Cool", "Funny", "Excited", "Calm", "Bright", "Dark",
        "Strong", "Weak", "Big", "Small", "Long", "Short", "Hot", "Cold",
        "Sweet", "Bitter", "Spicy", "Sour", "Soft", "Rough", "Pretty", "Ugly",
        "Slim", "Fat", "Tall", "Short", "Young", "Old", "New", "Ancient",
        "Clean", "Dirty", "Clear", "Cloudy", "Fragrant", "Loud", "Silent", "Colorful",
        "Fierce", "Gentle", "Wild", "Tame", "Mighty", "Tiny", "Swift", "Lazy",
        "Clever", "Silly", "Bold", "Shy", "Proud", "Humble", "Royal", "Noble"
    )

    private val animals = listOf(
        "Cat", "Dog", "Rabbit", "Hamster", "Panda", "Koala", "Penguin", "Dolphin",
        "Lion", "Tiger", "Bear", "Fox", "Wolf", "Squirrel", "Otter", "Raccoon",
        "Giraffe", "Elephant", "Hippo", "Rhino", "Zebra", "Kangaroo", "Monkey", "Chimp",
        "Parrot", "Eagle", "Owl", "Sparrow", "Pigeon", "Crow", "Magpie", "Swan",
        "Turtle", "Croc", "Snake", "Lizard", "Frog", "Tadpole", "Fish", "Shark",
        "Whale", "Octopus", "Squid", "Shrimp", "Crab", "Cow", "Pig", "Horse",
        "Sheep", "Goat", "Chicken", "Duck", "Goose", "Turkey", "Ostrich", "Mouse",
        "Deer", "Moose", "Buffalo", "Cheetah", "Leopard", "Panther", "Jaguar", "Lynx"
    )

    /**
     * Generate a random nickname pair with pattern: Adjective + Space + Animal
     * Example: "Fast Cat" (display), "fast_cat" (key)
     */
    private fun generateNicknamePair(): Pair<String, String> {
        val adjective = adjectives[Random.nextInt(adjectives.size)]
        val animal = animals[Random.nextInt(animals.size)]
        val nickname = "$adjective $animal"  // Display with space
        val nicknameKey = "${adjective.lowercase()}_${animal.lowercase()}"  // Key with underscore
        return Pair(nickname, nicknameKey)
    }

    /**
     * Generate a unique nickname by checking against existing users
     * If duplicate key, appends a random 4-digit number (e.g., "fast_cat_1234")
     */
    fun generateUniqueNickname(userRepository: UserRepository): NicknameResult {
        var (nickname, nicknameKey) = generateNicknamePair()

        // Check if nicknameKey already exists
        if (userRepository.existsByNicknameKey(nicknameKey)) {
            // Add random 4-digit suffix for collision
            val suffix = Random.nextInt(1000, 10000)
            nicknameKey = "${nicknameKey}_$suffix"
        }

        // Final safety check - if still exists, keep regenerating
        var attempts = 0
        while (userRepository.existsByNicknameKey(nicknameKey) && attempts < 10) {
            val suffix = Random.nextInt(1000, 10000)
            val (newNickname, baseKey) = generateNicknamePair()
            nickname = newNickname
            nicknameKey = "${baseKey}_$suffix"
            attempts++
        }

        return NicknameResult(nickname, nicknameKey)
    }
}
