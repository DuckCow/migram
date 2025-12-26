package com.example.backend.web.model

/**
 * Request DTO for translation
 */
data class TranslationRequestDTO(
    val contentType: ContentType,
    val contentId: Long,
    val targetLang: String
)

/**
 * Response DTO for translation
 */
data class TranslationResponseDTO(
    val contentType: ContentType,
    val contentId: Long,
    val sourceLang: String,
    val targetLang: String,
    val translatedText: String,
    val model: String,
    val cached: Boolean
)

/**
 * Request to external translation service (FastAPI)
 */
data class ExternalTranslationRequest(
    val text: String,
    val sourceLang: String,
    val targetLang: String,
    val quality: String = "best"
)

/**
 * Response from external translation service (FastAPI)
 */
data class ExternalTranslationResponse(
    val translatedText: String,
    val model: String
)
