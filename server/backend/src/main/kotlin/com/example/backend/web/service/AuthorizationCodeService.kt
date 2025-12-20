package com.example.backend.web.service

import com.example.backend.web.model.AuthorizationCode
import com.example.backend.web.repository.AuthorizationCodeRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class AuthorizationCodeService(
    private val authorizationCodeRepository: AuthorizationCodeRepository
) {

    private val secureRandom = SecureRandom()
    private val codeValidityMinutes = 5L // 5분간 유효

    /**
     * 안전한 랜덤 authorization code 생성
     */
    fun generateCode(email: String): String {
        val code = generateSecureRandomCode()
        val expiresAt = LocalDateTime.now().plusMinutes(codeValidityMinutes)

        val authorizationCode = AuthorizationCode(
            code = code,
            email = email,
            expiresAt = expiresAt
        )

        authorizationCodeRepository.save(authorizationCode)
        return code
    }

    /**
     * Code를 검증하고 사용 처리
     * @return email if valid, null if invalid/expired/already used
     */
    fun validateAndUseCode(code: String): String? {
        val authCode = authorizationCodeRepository.findByCode(code)
            ?: return null

        if (!authCode.isValid()) {
            return null
        }

        // 코드를 사용 처리
        authCode.used = true
        authorizationCodeRepository.save(authCode)

        return authCode.email
    }

    /**
     * 만료된 코드를 매일 정리 (매일 자정에 실행)
     */
    @Scheduled(cron = "0 0 0 * * *")
    fun cleanupExpiredCodes() {
        val now = LocalDateTime.now()
        authorizationCodeRepository.deleteByExpiresAtBefore(now)
    }

    /**
     * 안전한 랜덤 코드 생성 (Base64 URL-safe, 32바이트)
     */
    private fun generateSecureRandomCode(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
