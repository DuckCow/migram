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
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class TranslationService(
    private val contentTranslationRepository: ContentTranslationRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    @Value("\${translation.base-url:http://localhost:8001}")
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
     * Uses database cache if available, otherwise calls external translation service
     */
    @Transactional
    fun getTranslation(request: TranslationRequestDTO): TranslationResponseDTO {
        val (contentType, contentId, targetLang) = request

        // 1. Check cache first
        val cachedTranslation = contentTranslationRepository
            .findByContentTypeAndContentIdAndTargetLang(contentType, contentId, targetLang)

        if (cachedTranslation.isPresent) {
            logger.info("Translation cache HIT: $contentType#$contentId -> $targetLang")
            val cached = cachedTranslation.get()
            return TranslationResponseDTO(
                contentType = cached.contentType,
                contentId = cached.contentId,
                sourceLang = getOriginalLang(contentType, contentId),
                targetLang = cached.targetLang,
                translatedText = cached.translatedText,
                model = cached.model,
                cached = true
            )
        }

        logger.info("Translation cache MISS: $contentType#$contentId -> $targetLang")

        // 2. Load original content
        val (originalText, originalLang) = getOriginalContent(contentType, contentId)

        // 3. If source and target are the same, return original as cached
        if (originalLang == targetLang) {
            logger.info("Source and target language are the same ($originalLang), returning original")
            return TranslationResponseDTO(
                contentType = contentType,
                contentId = contentId,
                sourceLang = originalLang,
                targetLang = targetLang,
                translatedText = originalText,
                model = "none",
                cached = true
            )
        }

        // 4. Call external translation service
        val (translatedText, model) = callExternalTranslationService(
            text = originalText,
            sourceLang = originalLang,
            targetLang = targetLang
        )

        // 5. Save to cache (with concurrency handling)
        try {
            val savedTranslation = contentTranslationRepository.save(
                ContentTranslation(
                    contentType = contentType,
                    contentId = contentId,
                    targetLang = targetLang,
                    translatedText = translatedText,
                    model = model
                )
            )
            logger.info("Translation saved to cache: $contentType#$contentId -> $targetLang")

            return TranslationResponseDTO(
                contentType = contentType,
                contentId = contentId,
                sourceLang = originalLang,
                targetLang = targetLang,
                translatedText = translatedText,
                model = model,
                cached = false
            )
        } catch (e: DataIntegrityViolationException) {
            // Handle race condition: another request already saved the same translation
            logger.warn("Race condition detected, re-reading from cache: $contentType#$contentId -> $targetLang")
            val rereadTranslation = contentTranslationRepository
                .findByContentTypeAndContentIdAndTargetLang(contentType, contentId, targetLang)
                .orElseThrow { IllegalStateException("Translation should exist after conflict") }

            return TranslationResponseDTO(
                contentType = contentType,
                contentId = contentId,
                sourceLang = originalLang,
                targetLang = targetLang,
                translatedText = rereadTranslation.translatedText,
                model = rereadTranslation.model,
                cached = true
            )
        }
    }

    /**
     * Get original content and language
     */
    private fun getOriginalContent(contentType: ContentType, contentId: Long): Pair<String, String> {
        return when (contentType) {
            ContentType.POST -> {
                val post = postRepository.findById(contentId)
                    .orElseThrow { IllegalArgumentException("Post not found: $contentId") }
                // Combine title and content for posts
                val fullText = "${post.title}\n\n${post.content}"
                Pair(fullText, post.originalLang)
            }
            ContentType.COMMENT -> {
                val comment = commentRepository.findById(contentId)
                    .orElseThrow { IllegalArgumentException("Comment not found: $contentId") }
                Pair(comment.content, comment.originalLang)
            }
        }
    }

    /**
     * Get original language only (for response DTO)
     */
    private fun getOriginalLang(contentType: ContentType, contentId: Long): String {
        return when (contentType) {
            ContentType.POST -> {
                val post = postRepository.findById(contentId)
                    .orElseThrow { IllegalArgumentException("Post not found: $contentId") }
                post.originalLang
            }
            ContentType.COMMENT -> {
                val comment = commentRepository.findById(contentId)
                    .orElseThrow { IllegalArgumentException("Comment not found: $contentId") }
                comment.originalLang
            }
        }
    }

    /**
     * Call external translation service (FastAPI)
     */
    private fun callExternalTranslationService(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Pair<String, String> {
        try {
            logger.info("Calling external translation service: $sourceLang -> $targetLang")

            val request = ExternalTranslationRequest(
                text = text,
                sourceLang = sourceLang,
                targetLang = targetLang,
                quality = "best"
            )

            val response = webClient.post()
                .uri("/translate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ExternalTranslationResponse::class.java)
                .timeout(Duration.ofMillis(translationTimeoutMs))
                .onErrorResume { error ->
                    logger.error("Translation service error", error)
                    // Return fallback response
                    Mono.just(
                        ExternalTranslationResponse(
                            translatedText = "Translation service unavailable",
                            model = "error"
                        )
                    )
                }
                .block()

            return Pair(response?.translatedText ?: "Translation failed", response?.model ?: "error")

        } catch (e: WebClientResponseException) {
            logger.error("Translation API error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw RuntimeException("Translation service is unavailable. Please try again later.")
        } catch (e: Exception) {
            logger.error("Unexpected translation error", e)
            throw RuntimeException("Translation failed. Please try again later.")
        }
    }
}
