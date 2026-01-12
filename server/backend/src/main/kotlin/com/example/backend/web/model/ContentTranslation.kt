package com.example.backend.web.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "content_translation",
    uniqueConstraints = [
        UniqueConstraint(
            name = "unique_translation",
            columnNames = ["content_type", "content_id", "target_lang"]
        )
    ]
)
data class ContentTranslation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "content_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val contentType: ContentType,

    @Column(name = "content_id", nullable = false)
    val contentId: Long,

    @Column(name = "target_lang", nullable = false, length = 10)
    val targetLang: String,

    @Column(name = "translated_text", nullable = false, columnDefinition = "TEXT")
    val translatedText: String,

    @Column(name = "model", nullable = false, length = 100)
    val model: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class ContentType {
    POST,
    COMMENT
}
