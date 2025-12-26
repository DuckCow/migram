package com.example.backend.web.controller

import com.example.backend.web.model.TranslationRequestDTO
import com.example.backend.web.model.TranslationResponseDTO
import com.example.backend.web.service.TranslationService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/translations")
class TranslationController(
    private val translationService: TranslationService
) {
    private val logger = LoggerFactory.getLogger(TranslationController::class.java)

    /**
     * POST /api/translations
     * Get translation for post or comment content
     * Returns cached result if available, otherwise translates and caches
     */
    @PostMapping
    fun getTranslation(@RequestBody request: TranslationRequestDTO): ResponseEntity<TranslationResponseDTO> {
        return try {
            logger.info("Translation request: ${request.contentType}#${request.contentId} -> ${request.targetLang}")
            val response = translationService.getTranslation(request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            // Content not found
            logger.warn("Content not found: ${e.message}")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            // Translation service error - return user-friendly message
            logger.error("Translation error", e)
            ResponseEntity.status(500).build()
        }
    }
}
