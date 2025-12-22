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

        if (user.nickname == null) {
            val nickname = nicknameGeneratorService.generateUniqueNickname(userRepository)
            val updatedUser = user.copy(nickname = nickname)
            return userRepository.save(updatedUser)
        }

        return user
    }

    /**
     * Update user's nickname (manual change)
     * Validates English letters only, 3-30 characters
     */
    fun updateNickname(userId: Long, newNickname: String): User {
        // Validate nickname format (English letters only, 3-30 chars)
        if (!newNickname.matches(Regex("^[A-Za-z]{3,30}$"))) {
            throw IllegalArgumentException("Nickname must be 3-30 English letters")
        }

        // Check uniqueness
        if (userRepository.existsByNickname(newNickname)) {
            throw IllegalArgumentException("Nickname already exists")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val updatedUser = user.copy(nickname = newNickname)
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
            // Validate nickname format (English letters only, 3-30 chars)
            if (!nickname.matches(Regex("^[A-Za-z]{3,30}$"))) {
                throw IllegalArgumentException("Nickname must be 3-30 English letters")
            }

            // Check uniqueness
            if (userRepository.existsByNickname(nickname)) {
                throw IllegalArgumentException("Nickname already exists")
            }

            updatedUser = updatedUser.copy(nickname = nickname)
        }

        // Update countries
        updatedUser = updatedUser.copy(
            originCountry = originCountry,
            currentCountry = currentCountry
        )

        return userRepository.save(updatedUser).toDTO()
    }
}
