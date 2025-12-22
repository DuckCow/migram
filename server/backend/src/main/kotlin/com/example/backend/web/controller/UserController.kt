package com.example.backend.web.controller

import com.example.backend.web.model.ProfileUpdateDTO
import com.example.backend.web.model.UserDTO
import com.example.backend.web.model.UserProfileDTO
import com.example.backend.web.service.UserService
import com.example.backend.web.util.SecurityUtils
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val securityUtils: SecurityUtils
) {

    @GetMapping("/me")
    fun getCurrentUser(): UserDTO {
        val userId = securityUtils.getCurrentUserId()
        return userService.findById(userId)
    }

    @GetMapping("/me/profile")
    fun getCurrentUserProfile(): UserProfileDTO {
        val userId = securityUtils.getCurrentUserId()
        return userService.getUserProfile(userId)
    }

    @PutMapping("/me")
    fun updateCurrentUserProfile(@RequestBody profileUpdateDTO: ProfileUpdateDTO): UserDTO {
        val userId = securityUtils.getCurrentUserId()
        return userService.updateProfile(
            userId,
            profileUpdateDTO.nickname,
            profileUpdateDTO.originCountry,
            profileUpdateDTO.currentCountry
        )
    }

    @GetMapping
    fun getAllUsers(): List<UserDTO> = userService.findAll()

    @GetMapping("/{userId}")
    fun getUserById(@PathVariable userId: Long): UserDTO = userService.findById(userId)

    @PostMapping
    fun createUser(@RequestBody userDTO: UserDTO): UserDTO = userService.save(userDTO)

    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: Long,
        @RequestBody userDTO: UserDTO
    ): UserDTO = userService.update(userId, userDTO)

    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: Long) {
        userService.delete(userId)
    }
}