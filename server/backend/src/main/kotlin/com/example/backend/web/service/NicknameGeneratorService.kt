package com.example.backend.web.service

import com.example.backend.web.repository.UserRepository
import org.springframework.stereotype.Service
import kotlin.random.Random

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
     * Generate a random nickname with pattern: Adjective + Animal
     * Example: "FastCat", "HappyPanda", "BraveWolf"
     */
    fun generateNickname(): String {
        val adjective = adjectives[Random.nextInt(adjectives.size)]
        val animal = animals[Random.nextInt(animals.size)]
        return "$adjective$animal"
    }

    /**
     * Generate a unique nickname by checking against existing users
     * If duplicate, appends a number (e.g., "빠른고양이2")
     */
    fun generateUniqueNickname(userRepository: UserRepository): String {
        var nickname = generateNickname()
        var counter = 2

        // Keep trying until we find a unique nickname
        while (userRepository.existsByNickname(nickname)) {
            nickname = "${generateNickname()}$counter"
            counter++

            // Safety check: if counter gets too high, just use a timestamp
            if (counter > 100) {
                nickname = "${generateNickname()}${System.currentTimeMillis() % 10000}"
                break
            }
        }

        return nickname
    }
}
