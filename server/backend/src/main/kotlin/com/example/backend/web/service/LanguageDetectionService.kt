package com.example.backend.web.service

import com.example.backend.web.model.LanguageDetector
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class LanguageDetectorService(
    @Qualifier("customLanguageDetector") private val languageDetector: LanguageDetector

) {
    fun detectLanguage(text: String): String {
        return try {
            val detectedLanguage = languageDetector.detect(text)
                ?: throw LanguageDetectionException("Cannot detect language")
            detectedLanguage
        } catch (e: Exception) {
            throw LanguageDetectionException("Language detection failed: ${e.message}")
        }
    }
}

class LanguageDetectionException(message: String) : RuntimeException(message)