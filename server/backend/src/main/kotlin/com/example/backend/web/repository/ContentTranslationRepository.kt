package com.example.backend.web.repository

import com.example.backend.web.model.ContentTranslation
import com.example.backend.web.model.ContentType
import com.example.backend.web.model.TranslationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ContentTranslationRepository : JpaRepository<ContentTranslation, Long> {

    /**
     * Find cached translation by content type, ID, and target language (legacy, for backward compatibility)
     */
    fun findByContentTypeAndContentIdAndTargetLang(
        contentType: ContentType,
        contentId: Long,
        targetLang: String
    ): Optional<ContentTranslation>

    /**
     * Find cached translation by content type, ID, target language, and translation type
     */
    fun findByContentTypeAndContentIdAndTargetLangAndTranslationType(
        contentType: ContentType,
        contentId: Long,
        targetLang: String,
        translationType: TranslationType
    ): Optional<ContentTranslation>

    /**
     * Delete stale translation (when source text hash doesn't match)
     */
    fun deleteByContentTypeAndContentIdAndTargetLangAndTranslationType(
        contentType: ContentType,
        contentId: Long,
        targetLang: String,
        translationType: TranslationType
    )
}
