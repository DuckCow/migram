package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.CommentRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val nicknameGeneratorService: NicknameGeneratorService
) {

    fun findAll(): List<UserDTO> = userRepository.findAll().map { it.toDTO() }

    fun findById(userId: Long): UserDTO = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }.toDTO()

    /**
     * Get user profile with stats (post count, comment count)
     */
    fun getUserProfile(userId: Long): UserProfileDTO {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val postCount = postRepository.countByUserId(userId)
        val commentCount = commentRepository.countByUserId(userId)

        return UserProfileDTO(
            user = user.toDTO(),
            postCount = postCount,
            commentCount = commentCount
        )
    }

    fun save(userDTO: UserDTO): UserDTO {
        val user = userDTO.toEntity()
        return userRepository.save(user).toDTO()
    }

    fun update(userId: Long, userDTO: UserDTO): UserDTO {
        val existingUser = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val updatedUser = existingUser.copy(
            name = userDTO.name,
            email = userDTO.email,
            profilePicture = userDTO.profilePicture
        )
        return userRepository.save(updatedUser).toDTO()
    }

    fun delete(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        userRepository.delete(user)
    }

    fun findEntityById(userId: Long): User? = userRepository.findById(userId).orElse(null)

    // findByEmail 메서드 추가 - DTO 반환
    fun findByEmail(email: String): UserDTO? {
        return userRepository.findByEmail(email)?.toDTO()
    }

    // findByEmail 메서드 추가 - Entity 반환 (JWT 인증에서 사용)
    fun findEntityByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }
    fun updateUserCountries(userId: Long, originCountry: Country?, currentCountry: Country?) {
        val user = findEntityById(userId) ?: throw IllegalArgumentException("User not found with id: $userId")
        user.originCountry = originCountry
        user.currentCountry = currentCountry
        userRepository.save(user)
    }

    /**
     * Auto-assign a unique nickname to a user
     * Called during first login
     */
    fun assignNickname(userId: Long): User {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        if (user.nickname == null || user.nicknameKey == null) {
            val nicknameResult = nicknameGeneratorService.generateUniqueNickname(userRepository)
            val updatedUser = user.copy(
                nickname = nicknameResult.nickname,
                nicknameKey = nicknameResult.nicknameKey
            )
            return userRepository.save(updatedUser)
        }

        return user
    }

    /**
     * Update user's nickname (manual change)
     * Validates English letters and space only, 3-24 characters
     */
    fun updateNickname(userId: Long, newNickname: String): User {
        // Validate nickname format (English letters and space, 3-24 chars)
        val trimmedNickname = newNickname.trim()
        if (!trimmedNickname.matches(Regex("^[A-Za-z ]{3,24}$"))) {
            throw IllegalArgumentException("Nickname must be 3-24 English letters (space allowed)")
        }

        // Generate nicknameKey from nickname
        var nicknameKey = trimmedNickname.lowercase().replace(" ", "_")

        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        // Check uniqueness of nicknameKey (skip if it's the same user)
        if (userRepository.existsByNicknameKey(nicknameKey)) {
            val existingUser = userRepository.findAll().find { it.nicknameKey == nicknameKey }
            if (existingUser?.id != userId) {
                // Add random 4-digit suffix for collision
                val suffix = kotlin.random.Random.nextInt(1000, 10000)
                nicknameKey = "${nicknameKey}_$suffix"
            }
        }

        val updatedUser = user.copy(
            nickname = trimmedNickname,
            nicknameKey = nicknameKey
        )
        return userRepository.save(updatedUser)
    }

    /**
     * Update user profile (nickname and countries)
     */
    fun updateProfile(userId: Long, nickname: String?, originCountry: Country?, currentCountry: Country?): UserDTO {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        var updatedUser = user

        // Update nickname if provided and different
        if (nickname != null && nickname != user.nickname) {
            val trimmedNickname = nickname.trim()
            // Validate nickname format (English letters and space, 3-24 chars)
            if (!trimmedNickname.matches(Regex("^[A-Za-z ]{3,24}$"))) {
                throw IllegalArgumentException("Nickname must be 3-24 English letters (space allowed)")
            }

            // Generate nicknameKey from nickname
            var nicknameKey = trimmedNickname.lowercase().replace(" ", "_")

            // Check uniqueness of nicknameKey (skip if it's the same user)
            if (userRepository.existsByNicknameKey(nicknameKey)) {
                val existingUser = userRepository.findAll().find { it.nicknameKey == nicknameKey }
                if (existingUser?.id != userId) {
                    // Add random 4-digit suffix for collision
                    val suffix = kotlin.random.Random.nextInt(1000, 10000)
                    nicknameKey = "${nicknameKey}_$suffix"
                }
            }

            updatedUser = updatedUser.copy(
                nickname = trimmedNickname,
                nicknameKey = nicknameKey
            )
        }

        // Update countries
        updatedUser = updatedUser.copy(
            originCountry = originCountry,
            currentCountry = currentCountry
        )

        return userRepository.save(updatedUser).toDTO()
    }

    /**
     * Update user's DM privacy setting
     */
    fun updateDmPrivacy(userId: Long, allowDmsFrom: DmPrivacySetting): UserDTO {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        val updatedUser = user.copy(allowDmsFrom = allowDmsFrom)
        return userRepository.save(updatedUser).toDTO()
    }
}
