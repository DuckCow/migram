package com.example.backend.web.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * OAuth2 인증 후 발급되는 일회용 authorization code
 * 보안을 위해 토큰을 URL에 직접 노출하지 않고 code를 통해 교환
 */
@Entity
@Table(name = "authorization_codes")
data class AuthorizationCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 64)
    val code: String,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false)
    val expiresAt: LocalDateTime,

    @Column(nullable = false)
    var used: Boolean = false,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)
    fun isValid(): Boolean = !used && !isExpired()
}
