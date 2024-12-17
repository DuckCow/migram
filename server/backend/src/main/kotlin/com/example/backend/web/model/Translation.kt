package com.example.backend.web.model

class Translation {
}

// 번역 요청을 위한 DTO
data class TranslationRequest(
    val text: String,
    val targetLang: String? = null // 목표 언어가 지정되지 않은 경우 자동 감지
)

data class TranslationResponse(
    val originalText: String,
    val detectedLanguage: String,
    val translatedText: String,
    val targetLanguage: String
)
