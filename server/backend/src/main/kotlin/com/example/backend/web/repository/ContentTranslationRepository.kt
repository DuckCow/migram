package com.example.backend.web.repository

import com.example.backend.web.model.ContentTranslation
import com.example.backend.web.model.ContentType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ContentTranslationRepository : JpaRepository<ContentTranslation, Long> {

    /**
     * Find cached translation by content type, ID, and target language
     */
    fun findByContentTypeAndContentIdAndTargetLang(
        contentType: ContentType,
        contentId: Long,
        targetLang: String
    ): Optional<ContentTranslation>
}
