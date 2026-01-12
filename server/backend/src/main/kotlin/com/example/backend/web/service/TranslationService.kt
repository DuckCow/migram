package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.ContentTranslationRepository
import com.example.backend.web.repository.CommentRepository
import com.example.backend.web.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Service
class TranslationService(
    private val contentTranslationRepository: ContentTranslationRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    @Value("\${translation.base-url:http://localhost:5001}")
    private val translationBaseUrl: String,
    @Value("\${translation.timeout-ms:8000}")
    private val translationTimeoutMs: Long
) {
    private val logger = LoggerFactory.getLogger(TranslationService::class.java)
    private val webClient: WebClient = WebClient.builder()
        .baseUrl(translationBaseUrl)
        .build()

    /**
     * Get translation for content (post or comment)
     * Simple flow: check cache -> call external service -> save cache
     */
    @Transactional
    fun getTranslation(request: TranslationRequestDTO): TranslationResponseDTO {
        val contentType = request.contentType
        val contentId = request.contentId
        val targetLang = request.targetLang

        // 1. Get original content from DB
        val (originalText, originalLang) = getOriginalContent(contentType, contentId)

        // 2. If same language, return original (no translation needed)
        if (originalLang == targetLang) {
            logger.info("Same language ($originalLang), skipping translation")
            return TranslationResponseDTO(
                contentType = contentType,
                contentId = contentId,
                sourceLang = originalLang,
                targetLang = targetLang,
                translatedText = originalText,
                model = "none",
                cached = true,
                skipped = true
            )
        }

        // 3. Check DB cache
        val cached = contentTranslationRepository
            .findByContentTypeAndContentIdAndTargetLang(contentType, contentId, targetLang)

        if (cached.isPresent) {
            val entry = cached.get()
            logger.info("Cache HIT: $contentType#$contentId -> $targetLang")
            return TranslationResponseDTO(
                contentType = entry.contentType,
                contentId = entry.contentId,
                sourceLang = originalLang,
                targetLang = entry.targetLang,
                translatedText = entry.translatedText,
                model = entry.model,
                cached = true
            )
        }

        logger.info("Cache MISS: $contentType#$contentId -> $targetLang")

        // 4. Call external translation service
        val (translatedText, model) = callTranslationService(originalText, originalLang, targetLang)

        // 5. Save to cache (handle concurrent inserts)
        return saveToCache(
            contentType = contentType,
            contentId = contentId,
            targetLang = targetLang,
            sourceLang = originalLang,
            translatedText = translatedText,
            model = model
        )
    }

    private fun getOriginalContent(contentType: ContentType, contentId: Long): Pair<String, String> {
        return when (contentType) {
            ContentType.POST -> {
                val post = postRepository.findById(contentId)
                    .orElseThrow { IllegalArgumentException("Post not found: $contentId") }
                Pair("${post.title}\n\n${post.content}", post.originalLang)
            }
            ContentType.COMMENT -> {
                val comment = commentRepository.findById(contentId)
                    .orElseThrow { IllegalArgumentException("Comment not found: $contentId") }
                Pair(comment.content, comment.originalLang)
            }
        }
    }

    private fun callTranslationService(text: String, sourceLang: String, targetLang: String): Pair<String, String> {
        logger.info("Calling translation service: $sourceLang -> $targetLang")

        try {
            val response = webClient.post()
                .uri("/translate")
                .bodyValue(ExternalTranslationRequest(
                    text = text,
                    sourceLang = sourceLang,
                    targetLang = targetLang
                ))
                .retrieve()
                .bodyToMono(ExternalTranslationResponse::class.java)
                .timeout(Duration.ofMillis(translationTimeoutMs))
                .block()

            if (response == null) {
                throw RuntimeException("Translation service returned null")
            }

            return Pair(response.translatedText, response.model ?: "unknown")
        } catch (e: Exception) {
            logger.error("Translation service error: ${e.message}")
            throw RuntimeException("Translation service unavailable. Please try again later.")
        }
    }

    private fun saveToCache(
        contentType: ContentType,
        contentId: Long,
        targetLang: String,
        sourceLang: String,
        translatedText: String,
        model: String
    ): TranslationResponseDTO {
        try {
            contentTranslationRepository.save(
                ContentTranslation(
                    contentType = contentType,
                    contentId = contentId,
                    targetLang = targetLang,
                    translatedText = translatedText,
                    model = model
                )
            )
            logger.info("Cached translation: $contentType#$contentId -> $targetLang")

            return TranslationResponseDTO(
                contentType = contentType,
                contentId = contentId,
                sourceLang = sourceLang,
                targetLang = targetLang,
                translatedText = translatedText,
                model = model,
                cached = false
            )
        } catch (e: DataIntegrityViolationException) {
            // Concurrent insert - read the cached entry
            logger.warn("Concurrent insert detected, reading from cache")
            val cached = contentTranslationRepository
                .findByContentTypeAndContentIdAndTargetLang(contentType, contentId, targetLang)
                .orElseThrow { IllegalStateException("Cache entry should exist after conflict") }

            return TranslationResponseDTO(
                contentType = cached.contentType,
                contentId = cached.contentId,
                sourceLang = sourceLang,
                targetLang = cached.targetLang,
                translatedText = cached.translatedText,
                model = cached.model,
                cached = true
            )
        }
    }
}
