package com.example.backend.web.controller

import com.example.backend.web.model.CustomOAuth2User
import com.example.backend.web.service.UserService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/auth") // 이 줄 추가!
class AuthController(
    private val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)



    @PostMapping("/token")
    fun exchangeToken(@RequestBody request: TokenRequest): ResponseEntity<TokenResponse> {
        logger.info("Token exchange request received: $request")
        try {
            val accessToken = "임시 액세스 토큰"
            val refreshToken = "임시 리프레시 토큰"

            logger.info("Returning tokens")
            return ResponseEntity.ok(TokenResponse(accessToken, refreshToken))
        } catch (e: Exception) {
            logger.error("Error during token exchange", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    data class TokenRequest(val code: String)
    data class TokenResponse(val accessToken: String, val refreshToken: String)

    @GetMapping("/profile")
    fun checkUserProfile(authentication: Authentication?): ResponseEntity<ProfileStatusResponse> {
        println("=== checkUserProfile called ===")
        println("Authentication: $authentication")

        if (authentication == null || !authentication.isAuthenticated) {
            println("Authentication is null or not authenticated")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return try {
            val user = when (val principal = authentication.principal) {
                is CustomOAuth2User -> {
                    println("OAuth2 authentication - getting user from CustomOAuth2User")
                    principal.getUser()
                }
                is String -> {
                    // JWT 인증의 경우 - principal이 email 문자열
                    println("JWT authentication - email: $principal")
                    userService.findEntityByEmail(principal)
                }
                else -> {
                    println("Unknown principal type: ${principal?.javaClass}")
                    null
                }
            }

            if (user == null) {
                println("User not found")
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }

            println("User found: ${user.email}")

            val isProfileComplete = user.originCountry != null && user.currentCountry != null

            ResponseEntity.ok(ProfileStatusResponse(isProfileComplete))

        } catch (e: Exception) {
            println("Error in checkUserProfile: ${e.message}")
            e.printStackTrace()
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }


    @GetMapping("/oauth2/authorization/google")
    fun googleLogin(request: HttpServletRequest): String? {
        val userAgent = request.getHeader("User-Agent")
        return if (userAgent != null && (userAgent.contains("Android") || userAgent.contains("iOS"))) {
            "redirect:migram://oauth/callback"
        } else "redirect:/oauth2/authorization/google"
        // 웹 브라우저의 경우 기본 OAuth2 리디렉션 URI 사용
    }

    @GetMapping("/login/oauth2/code/google")
    fun googleCallback(@RequestParam code: String?): String? {
        // 인증 코드를 받아서 처리하는 로직
        return "redirect:/success" // 또는 적절한 페이지로 리디렉션
    }

    data class ProfileStatus(
        val isProfileComplete: Boolean,
        val originCountry: String?,
        val currentCountry: String?
    )

}

data class ProfileStatusResponse(val isProfileComplete: Boolean)


