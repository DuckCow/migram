package com.example.backend.web.controller

import com.example.backend.web.model.CustomOAuth2User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
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

    data class ProfileStatus(
        val isProfileComplete: Boolean,
        val originCountry: String?,
        val currentCountry: String?
    )
}