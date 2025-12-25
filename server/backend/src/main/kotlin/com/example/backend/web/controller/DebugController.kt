package com.example.backend.web.controller

import com.example.backend.web.model.CustomOAuth2User
import com.example.backend.web.model.jwt.JwtTokenProvider
import com.example.backend.web.repository.FollowRepository
import com.example.backend.web.repository.UserRepository
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/debug")
@Profile("dev")  // Only available in development
class DebugController(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @GetMapping("/follows")
    fun getAllFollows(): List<Map<String, Any?>> {
        return followRepository.findAll().map { follow ->
            mapOf(
                "id" to follow.id,
                "requester" to mapOf(
                    "id" to follow.requester.id,
                    "nickname" to (follow.requester.nickname ?: ""),
                    "email" to follow.requester.email
                ),
                "target" to mapOf(
                    "id" to follow.target.id,
                    "nickname" to (follow.target.nickname ?: ""),
                    "email" to follow.target.email
                ),
                "status" to follow.status.name,
                "createdAt" to follow.createdAt.toString(),
                "respondedAt" to (follow.respondedAt?.toString() ?: "")
            )
        }
    }

    @GetMapping("/users")
    fun getAllUsers(): List<Map<String, Any?>> {
        return userRepository.findAll().map { user ->
            mapOf(
                "id" to user.id,
                "name" to user.name,
                "email" to user.email,
                "nickname" to user.nickname
            )
        }
    }

    @GetMapping("/token/{userId}")
    fun generateToken(@PathVariable userId: Long): Map<String, String> {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }
        val authentication = UsernamePasswordAuthenticationToken(
            CustomOAuth2User(user),
            null,
            authorities
        )

        val token = jwtTokenProvider.generateAccessToken(authentication)
        return mapOf("token" to token, "userId" to userId.toString(), "email" to user.email)
    }
}
