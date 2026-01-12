package com.example.backend.web.model

import jakarta.persistence.*
import java.time.Instant

/**
 * Content visibility states for moderation
 */
enum class ContentVisibility {
    PUBLIC,    // Visible everywhere
    LIMITED,   // Visible but de-ranked in feeds
    HIDDEN,    // Visible only to author + admin
    REMOVED    // Admin-only, manual action only
}

/**
 * Types of moderation actions that can be taken
 */
enum class ModerationActionType {
    NO_ACTION,       // Report reviewed but no action needed
    SET_LIMITED,     // De-rank content
    SET_HIDDEN,      // Hide from public, visible to author
    SET_REMOVED,     // Remove completely (admin-only view)
    RESTORE_PUBLIC   // Restore to public visibility
}

/**
 * Report status for tracking review progress
 */
enum class ReportStatus {
    PENDING,   // Awaiting admin review
    RESOLVED,  // Reviewed and action taken
    REJECTED   // Reviewed and dismissed (no violation)
}

/**
 * Reasons for reporting content
 */
enum class ReportReason {
    SPAM,
    HARASSMENT,
    HATE_SPEECH,
    VIOLENCE,
    MISINFORMATION,
    INAPPROPRIATE,
    COPYRIGHT,
    OTHER
}

/**
 * User-submitted report on a post or comment
 */
@Entity
@Table(name = "report")
data class Report(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    val reporter: User,

    @Column(name = "content_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val contentType: ContentType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    val comment: Comment? = null,

    @Column(name = "reason", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val reason: ReportReason,

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val status: ReportStatus = ReportStatus.PENDING,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    val reviewedBy: User? = null,

    @Column(name = "reviewed_at")
    val reviewedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * Get the content ID regardless of type
     */
    fun getContentId(): Long = when (contentType) {
        ContentType.POST -> post?.id ?: throw IllegalStateException("Post is null for POST report")
        ContentType.COMMENT -> comment?.id ?: throw IllegalStateException("Comment is null for COMMENT report")
    }
}

/**
 * Audit log entry for moderation actions (immutable, append-only)
 */
@Entity
@Table(name = "moderation_action")
data class ModerationAction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "content_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val contentType: ContentType,

    @Column(name = "content_id", nullable = false)
    val contentId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    val report: Report? = null,

    @Column(name = "action_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    val actionType: ModerationActionType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    val admin: User,

    @Column(name = "note", columnDefinition = "TEXT")
    val note: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

// ==================== DTOs ====================

/**
 * Request to create a new report
 */
data class ReportRequestDTO(
    val contentType: ContentType,
    val contentId: Long,
    val reason: ReportReason,
    val description: String? = null
)

/**
 * Response for a report
 */
data class ReportResponseDTO(
    val id: Long,
    val reporterId: Long,
    val reporterName: String,
    val contentType: ContentType,
    val contentId: Long,
    val reason: ReportReason,
    val description: String?,
    val status: ReportStatus,
    val reviewedById: Long?,
    val reviewedByName: String?,
    val reviewedAt: String?,
    val createdAt: String
)

/**
 * Request to review a report (admin only)
 */
data class ReportReviewRequestDTO(
    val status: ReportStatus,
    val action: ModerationActionType,
    val adminNote: String? = null
)

/**
 * Response for moderation action audit log
 */
data class ModerationActionResponseDTO(
    val id: Long,
    val contentType: ContentType,
    val contentId: Long,
    val reportId: Long?,
    val actionType: ModerationActionType,
    val adminId: Long,
    val adminName: String,
    val note: String?,
    val createdAt: String
)
