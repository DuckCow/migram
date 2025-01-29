package com.example.backend.web.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomOAuth2User(private val user: com.example.backend.web.model.User) : OAuth2User {
    override fun getName(): String = user.name

    override fun getAttributes(): Map<String, Any> = mapOf(
        "id" to user.id,
        "email" to user.email,
        "name" to user.name,
        "picture" to (user.profilePicture ?: ""),
        "originCountry" to (user.originCountry?.name ?: ""),
        "currentCountry" to (user.currentCountry?.name ?: "")
    )

    override fun getAuthorities(): Collection<GrantedAuthority> =
        user.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }

    // User 객체에 직접 접근할 수 있는 메서드 추가 (필요한 경우)
    fun getUser(): com.example.backend.web.model.User = user

    // 역할 확인을 위한 편의 메서드 추가

    // originCountry와 currentCountry를 직접 접근할 수 있는 프로퍼티 추가
    val originCountry: Country?
        get() = user.originCountry

    val currentCountry: Country?
        get() = user.currentCountry

    fun hasRole(role: Role): Boolean = user.roles.contains(role)
}