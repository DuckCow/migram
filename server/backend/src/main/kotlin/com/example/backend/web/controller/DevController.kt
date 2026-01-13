package com.example.backend.web.controller

import com.example.backend.web.model.Role
import com.example.backend.web.model.jwt.JwtTokenProvider
import com.example.backend.web.repository.UserRepository
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Development-only endpoints for testing.
 * Only available when running with 'dev' profile.
 */
@RestController
@RequestMapping("/auth")
@Profile("dev")
class DevController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) {

    /**
     * Generate a JWT token for testing purposes.
     * Usage: GET /auth/dev-token?email=admin@test.com
     *
     * WARNING: This endpoint should NEVER be exposed in production!
     */
    @GetMapping("/dev-token")
    fun getDevToken(@RequestParam email: String): ResponseEntity<DevTokenResponse> {
        val user = userRepository.findByEmail(email)
            ?: return ResponseEntity.badRequest().body(
                DevTokenResponse(
                    success = false,
                    error = "User not found: $email"
                )
            )

        // Check if user has ADMIN role
        val isAdmin = user.roles.contains(Role.ADMIN)

        val token = jwtTokenProvider.generateAccessTokenForEmail(email)

        return ResponseEntity.ok(
            DevTokenResponse(
                success = true,
                token = token,
                userId = user.id,
                email = user.email,
                name = user.name,
                isAdmin = isAdmin,
                roles = user.roles.map { it.name }
            )
        )
    }

    /**
     * Create or get a test admin user for development.
     * Usage: POST /auth/dev-admin
     */
    @PostMapping("/dev-admin")
    fun createDevAdmin(): ResponseEntity<DevTokenResponse> {
        val adminEmail = "admin@migram.dev"

        // Find or create admin user
        var user = userRepository.findByEmail(adminEmail)

        if (user == null) {
            // Create new admin user
            user = userRepository.save(
                com.example.backend.web.model.User(
                    email = adminEmail,
                    name = "Dev Admin",
                    profilePicture = null,
                    originCountry = null,
                    currentCountry = null,
                    roles = mutableSetOf(Role.USER, Role.ADMIN)
                )
            )
        } else if (!user.roles.contains(Role.ADMIN)) {
            // Add ADMIN role if missing
            user = userRepository.save(
                user.copy(roles = (user.roles + Role.ADMIN).toMutableSet())
            )
        }

        val token = jwtTokenProvider.generateAccessTokenForEmail(adminEmail)

        return ResponseEntity.ok(
            DevTokenResponse(
                success = true,
                token = token,
                userId = user.id,
                email = user.email,
                name = user.name,
                isAdmin = true,
                roles = user.roles.map { it.name }
            )
        )
    }
}

data class DevTokenResponse(
    val success: Boolean,
    val token: String? = null,
    val userId: Long? = null,
    val email: String? = null,
    val name: String? = null,
    val isAdmin: Boolean? = null,
    val roles: List<String>? = null,
    val error: String? = null
)
