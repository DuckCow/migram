package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(private val userRepository: UserRepository) {

    fun findAll(): List<UserDTO> = userRepository.findAll().map { it.toDTO() }

    fun findById(userId: Long): UserDTO = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }.toDTO()

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
    fun updateUserCountries(userId: Long, originCountry: Country?, currentCountry: Country?) {
        val user = findEntityById(userId) ?: throw IllegalArgumentException("User not found with id: $userId")
        user.originCountry=originCountry;
        user.currentCountry=currentCountry;


    }
}
