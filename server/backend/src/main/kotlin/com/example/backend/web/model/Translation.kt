package com.example.backend.web.model

/**
 * Translation type: FULL (complete content) or PREVIEW (truncated for feed)
 */
enum class TranslationType {
    FULL,    // Full title + body (for post detail)
    PREVIEW  // Title + truncated body (for feed)
}

/**
 * Request DTO for translation
 */
data class TranslationRequestDTO(
    val contentType: ContentType,
    val contentId: Long,
    val targetLang: String,
    val sourceText: String? = null,  // Optional custom text (for preview)
    val translationType: TranslationType = TranslationType.FULL
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
    val cached: Boolean,
    val skipped: Boolean = false  // True if source == target language (no translation needed)
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
