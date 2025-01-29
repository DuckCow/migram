package com.example.backend.web.controller

import com.example.backend.web.model.CustomOAuth2User
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
class AuthController {
    @GetMapping("/api/user/check-profile")
    fun checkUserProfile(@AuthenticationPrincipal user: CustomOAuth2User): ProfileStatus {
        return ProfileStatus(
            isProfileComplete = !(user.originCountry == null || user.currentCountry == null),
            originCountry = user.originCountry?.name,
            currentCountry = user.currentCountry?.name
        )
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