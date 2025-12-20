package com.example.backend.web.util

import com.example.backend.web.model.CustomOAuth2User
import com.example.backend.web.service.UserService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component


@Component
class SecurityUtils(
    private val userService: UserService
) {

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     * JWT 인증과 OAuth2 인증을 모두 지원합니다.
     */
    fun getCurrentUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication found")

        return when (val principal = authentication.principal) {
            // OAuth2 로그인 (CustomOAuth2User)
            is CustomOAuth2User -> {
                principal.getUser().id
            }
            // JWT 토큰 인증 (email을 principal로 사용)
            is String -> {
                val email = principal
                val user = userService.findEntityByEmail(email)
                    ?: throw IllegalStateException("User not found with email: $email")
                user.id
            }
            else -> throw IllegalStateException("Unknown authentication principal type: ${principal::class.java}")
        }
    }

    /**
     * 현재 인증된 사용자의 이메일을 반환합니다.
     */
    fun getCurrentUserEmail(): String {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication found")

        return when (val principal = authentication.principal) {
            is CustomOAuth2User -> principal.getUser().email
            is String -> principal
            else -> throw IllegalStateException("Unknown authentication principal type")
        }
    }


}