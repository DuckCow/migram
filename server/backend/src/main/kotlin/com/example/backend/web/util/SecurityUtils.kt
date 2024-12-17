package com.example.backend.web.util

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component


@Component
class SecurityUtils {

    fun getCurrentUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.principal is OAuth2User) {
            val oauth2User = authentication.principal as OAuth2User
            return oauth2User.attributes["id"] as Long
        }
        throw IllegalStateException("No authenticated user found")
    }


}