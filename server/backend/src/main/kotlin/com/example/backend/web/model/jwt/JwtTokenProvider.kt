package com.example.backend.web.model.jwt

import com.example.backend.web.model.CustomOAuth2User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.access-token-validity-in-ms:3600000}")
    private val accessTokenValidityInMs: Long = 3600000L

    @Value("\${jwt.refresh-token-validity-in-ms:86400000}")
    private val refreshTokenValidityInMs: Long = 86400000L

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateAccessToken(authentication: Authentication): String {
        val oauth2User = authentication.principal as CustomOAuth2User
        val email = oauth2User.getUser().email // getUser()를 통해 접근

        return Jwts.builder()
            .subject(email)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessTokenValidityInMs))
            .signWith(secretKey) // 최신 방식
            .compact()
    }

    fun generateRefreshToken(authentication: Authentication): String {
        val oauth2User = authentication.principal as CustomOAuth2User
        val email = oauth2User.getUser().email // getUser()를 통해 접근

        return Jwts.builder()
            .subject(email)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + refreshTokenValidityInMs))
            .signWith(secretKey)
            .compact()
    }

    // 토큰 검증을 위한 메서드도 추가
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getEmailFromToken(token: String): String {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    }
}