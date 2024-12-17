package com.example.backend.web.controller

import com.example.backend.web.model.UserDTO
import com.example.backend.web.service.UserService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {



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