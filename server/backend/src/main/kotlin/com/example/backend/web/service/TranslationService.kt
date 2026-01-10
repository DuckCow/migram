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
import java.security.MessageDigest
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
     * Supports FULL and PREVIEW translation types with source text hash validation
     */
    @Transactional
    fun getTranslation(request: TranslationRequestDTO): TranslationResponseDTO {
        val (contentType, contentId, targetLang) = Triple(request.contentType, request.contentId, request.targetLang)
        val translationType = request.translationType

        // 1. Get original content (custom sourceText or from DB)
        val (originalText, originalLang) = getOriginalContent(request)

        // 2. Skip if source == target language
        if (originalLang == targetLang) {
            logger.info("Source and target language are the same ($originalLang), skipping translation")
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

        // 3. Compute hash of source text for cache validation
        val sourceHash = computeSha256(originalText)

        // 4. Check cache with translation type
        val cachedTranslation = contentTranslationRepository
            .findByContentTypeAndContentIdAndTargetLangAndTranslationType(
                contentType, contentId, targetLang, translationType
            )

        if (cachedTranslation.isPresent) {
            val cached = cachedTranslation.get()
            // Validate hash - if source text changed, invalidate cache
            if (cached.sourceTextHash == sourceHash) {
                logger.info("Translation cache HIT: $contentType#$contentId ($translationType) -> $targetLang")
                return TranslationResponseDTO(
                    contentType = cached.contentType,
                    contentId = cached.contentId,
                    sourceLang = originalLang,
                    targetLang = cached.targetLang,
                    translatedText = cached.translatedText,
                    model = cached.model,
                    cached = true
                )
            } else {
                // Hash mismatch - source text was edited, delete stale cache
                logger.info("Translation cache STALE (hash mismatch): $contentType#$contentId ($translationType)")
                contentTranslationRepository.delete(cached)
            }
        }

        logger.info("Translation cache MISS: $contentType#$contentId ($translationType) -> $targetLang")

        // 5. Call external translation service
        val (translatedText, model) = callExternalTranslationService(
            text = originalText,
            sourceLang = originalLang,
            targetLang = targetLang
        )

        // 6. Save to cache (with concurrency handling)
        try {
            contentTranslationRepository.save(
                ContentTranslation(
                    contentType = contentType,
                    contentId = contentId,
                    targetLang = targetLang,
                    translationType = translationType,
                    sourceTextHash = sourceHash,
                    translatedText = translatedText,
                    model = model
                )
            )
            logger.info("Translation saved to cache: $contentType#$contentId ($translationType) -> $targetLang")

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
            logger.warn("Race condition detected, re-reading from cache: $contentType#$contentId ($translationType)")
            val rereadTranslation = contentTranslationRepository
                .findByContentTypeAndContentIdAndTargetLangAndTranslationType(
                    contentType, contentId, targetLang, translationType
                )
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
     * Compute SHA-256 hash of text for cache validation
     */
    private fun computeSha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Get original content and language
     * Supports custom sourceText (for preview) or loads from DB
     */
    private fun getOriginalContent(request: TranslationRequestDTO): Pair<String, String> {
        // If custom sourceText provided (preview from client), use it
        if (request.sourceText != null) {
            val originalLang = when (request.contentType) {
                ContentType.POST -> {
                    postRepository.findById(request.contentId)
                        .orElseThrow { IllegalArgumentException("Post not found: ${request.contentId}") }
                        .originalLang
                }
                ContentType.COMMENT -> {
                    commentRepository.findById(request.contentId)
                        .orElseThrow { IllegalArgumentException("Comment not found: ${request.contentId}") }
                        .originalLang
                }
            }
            return Pair(request.sourceText, originalLang)
        }

        // Otherwise load from DB based on translation type
        return when (request.contentType) {
            ContentType.POST -> {
                val post = postRepository.findById(request.contentId)
                    .orElseThrow { IllegalArgumentException("Post not found: ${request.contentId}") }
                val text = when (request.translationType) {
                    TranslationType.PREVIEW -> {
                        // Truncate content for preview (max 200 chars)
                        val preview = if (post.content.length > 200) {
                            post.content.take(200) + "..."
                        } else {
                            post.content
                        }
                        "${post.title}\n\n$preview"
                    }
                    TranslationType.FULL -> "${post.title}\n\n${post.content}"
                }
                Pair(text, post.originalLang)
            }
            ContentType.COMMENT -> {
                val comment = commentRepository.findById(request.contentId)
                    .orElseThrow { IllegalArgumentException("Comment not found: ${request.contentId}") }
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
