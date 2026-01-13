package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ModerationService(
    private val reportRepository: ReportRepository,
    private val moderationActionRepository: ModerationActionRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {
    private val logger = LoggerFactory.getLogger(ModerationService::class.java)
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    // ==================== Report Creation ====================

    /**
     * Create a new report for a post or comment
     */
    fun createReport(request: ReportRequestDTO, reporterId: Long): ReportResponseDTO {
        val reporter = userRepository.findById(reporterId)
            .orElseThrow { IllegalArgumentException("User not found: $reporterId") }

        // Get the target content
        val (post, comment) = when (request.contentType) {
            ContentType.POST -> {
                val post = postRepository.findById(request.contentId)
                    .orElseThrow { IllegalArgumentException("Post not found: ${request.contentId}") }

                // Check if already reported by this user
                if (reportRepository.existsByReporterAndPost(reporter, post)) {
                    throw IllegalStateException("You have already reported this post")
                }

                Pair(post, null)
            }
            ContentType.COMMENT -> {
                val comment = commentRepository.findById(request.contentId)
                    .orElseThrow { IllegalArgumentException("Comment not found: ${request.contentId}") }

                // Check if already reported by this user
                if (reportRepository.existsByReporterAndComment(reporter, comment)) {
                    throw IllegalStateException("You have already reported this comment")
                }

                Pair(null, comment)
            }
        }

        val report = reportRepository.save(
            Report(
                reporter = reporter,
                contentType = request.contentType,
                post = post,
                comment = comment,
                reason = request.reason,
                description = request.description
            )
        )

        logger.info("Report created: ${report.id} by user $reporterId for ${request.contentType}#${request.contentId}")

        return report.toResponseDTO()
    }

    // ==================== Admin: Report Review ====================

    /**
     * Get pending reports for admin review
     */
    @Transactional(readOnly = true)
    fun getPendingReports(pageable: Pageable): Page<ReportResponseDTO> {
        return reportRepository.findByStatus(ReportStatus.PENDING, pageable)
            .map { it.toResponseDTO() }
    }

    /**
     * Get all reports with optional status filter
     */
    @Transactional(readOnly = true)
    fun getReports(status: ReportStatus?, pageable: Pageable): Page<ReportResponseDTO> {
        return if (status != null) {
            reportRepository.findByStatus(status, pageable)
        } else {
            reportRepository.findAll(pageable)
        }.map { it.toResponseDTO() }
    }

    /**
     * Get a single report by ID
     */
    @Transactional(readOnly = true)
    fun getReport(reportId: Long): ReportResponseDTO {
        val report = reportRepository.findById(reportId)
            .orElseThrow { IllegalArgumentException("Report not found: $reportId") }
        return report.toResponseDTO()
    }

    /**
     * Review a report and apply moderation action
     * This is the main admin endpoint for content moderation
     */
    fun reviewReport(reportId: Long, request: ReportReviewRequestDTO, adminId: Long): ReportResponseDTO {
        val admin = userRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("Admin not found: $adminId") }

        // Verify admin role
        if (!admin.roles.contains(Role.ADMIN)) {
            throw IllegalAccessException("User is not an admin")
        }

        val report = reportRepository.findById(reportId)
            .orElseThrow { IllegalArgumentException("Report not found: $reportId") }

        // Cannot review already processed reports
        if (report.status != ReportStatus.PENDING) {
            throw IllegalStateException("Report has already been reviewed")
        }

        // Validate action based on status
        validateReviewRequest(request)

        // Apply visibility change if action is not NO_ACTION
        if (request.action != ModerationActionType.NO_ACTION) {
            applyVisibilityChange(report.contentType, report.getContentId(), request.action)
        }

        // Update report status
        val updatedReport = reportRepository.save(
            report.copy(
                status = request.status,
                reviewedBy = admin,
                reviewedAt = Instant.now()
            )
        )

        // Log moderation action (audit trail)
        moderationActionRepository.save(
            ModerationAction(
                contentType = report.contentType,
                contentId = report.getContentId(),
                report = updatedReport,
                actionType = request.action,
                admin = admin,
                note = request.adminNote
            )
        )

        // Notify reporter that their report was resolved
        val statusMessage = when (request.status) {
            ReportStatus.RESOLVED -> "Your report has been resolved. ${if (request.action != ModerationActionType.NO_ACTION) "Action taken: ${request.action}" else "No action was needed."}"
            ReportStatus.REJECTED -> "Your report was reviewed and rejected."
            ReportStatus.PENDING -> "" // Should not happen due to validation
        }

        if (statusMessage.isNotEmpty()) {
            notificationService.createNotification(
                recipientId = report.reporter.id,
                actorId = adminId,
                type = NotificationType.REPORT_RESOLVED,
                referenceId = reportId,
                message = statusMessage
            )
        }

        logger.info("Report ${report.id} reviewed by admin $adminId: ${request.status} / ${request.action}")

        return updatedReport.toResponseDTO()
    }

    // ==================== Direct Moderation (without report) ====================

    /**
     * Apply moderation action directly without a report
     * For admin to proactively moderate content
     */
    fun moderateContent(
        contentType: ContentType,
        contentId: Long,
        action: ModerationActionType,
        adminId: Long,
        note: String? = null
    ): ModerationActionResponseDTO {
        val admin = userRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("Admin not found: $adminId") }

        // Verify admin role
        if (!admin.roles.contains(Role.ADMIN)) {
            throw IllegalAccessException("User is not an admin")
        }

        // Apply visibility change
        applyVisibilityChange(contentType, contentId, action)

        // Log moderation action
        val moderationAction = moderationActionRepository.save(
            ModerationAction(
                contentType = contentType,
                contentId = contentId,
                actionType = action,
                admin = admin,
                note = note
            )
        )

        logger.info("Content moderated by admin $adminId: $contentType#$contentId -> $action")

        return moderationAction.toResponseDTO()
    }

    // ==================== Audit Log ====================

    /**
     * Get moderation history for specific content
     */
    @Transactional(readOnly = true)
    fun getModerationHistory(contentType: ContentType, contentId: Long): List<ModerationActionResponseDTO> {
        return moderationActionRepository
            .findByContentTypeAndContentIdOrderByCreatedAtDesc(contentType, contentId)
            .map { it.toResponseDTO() }
    }

    /**
     * Get recent moderation actions (admin dashboard)
     */
    @Transactional(readOnly = true)
    fun getRecentActions(pageable: Pageable): Page<ModerationActionResponseDTO> {
        return moderationActionRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map { it.toResponseDTO() }
    }

    // ==================== Visibility Helpers ====================

    /**
     * Determine effective visibility for content display
     * Implements read-time cascade logic
     */
    @Transactional(readOnly = true)
    fun getEffectiveVisibility(post: Post): ContentVisibility {
        return post.visibility
    }

    /**
     * Determine effective visibility for a comment
     * If parent post is HIDDEN/REMOVED, comment is not visible
     * If parent comment is REMOVED, reply is not visible
     */
    @Transactional(readOnly = true)
    fun getEffectiveVisibility(comment: Comment): ContentVisibility {
        // Check post visibility first
        val postVisibility = comment.post.visibility
        if (postVisibility == ContentVisibility.HIDDEN || postVisibility == ContentVisibility.REMOVED) {
            return ContentVisibility.REMOVED // Inherit post's restricted visibility
        }

        // Check parent comment visibility (for replies)
        comment.parentComment?.let { parent ->
            if (parent.visibility == ContentVisibility.REMOVED) {
                return ContentVisibility.REMOVED
            }
        }

        return comment.visibility
    }

    /**
     * Check if content is visible to a specific user
     */
    fun isVisibleToUser(visibility: ContentVisibility, contentOwnerId: Long, viewerId: Long?, isAdmin: Boolean): Boolean {
        return when (visibility) {
            ContentVisibility.PUBLIC, ContentVisibility.LIMITED -> true
            ContentVisibility.HIDDEN -> isAdmin || viewerId == contentOwnerId
            ContentVisibility.REMOVED -> isAdmin
        }
    }

    // ==================== Private Helpers ====================

    private fun validateReviewRequest(request: ReportReviewRequestDTO) {
        when (request.status) {
            ReportStatus.REJECTED -> {
                if (request.action != ModerationActionType.NO_ACTION) {
                    throw IllegalArgumentException("REJECTED status can only have NO_ACTION")
                }
            }
            ReportStatus.RESOLVED -> {
                // Action is required for resolved reports
                // NO_ACTION is valid (reviewed but no moderation needed)
            }
            ReportStatus.PENDING -> {
                throw IllegalArgumentException("Cannot set status to PENDING when reviewing")
            }
        }
    }

    private fun applyVisibilityChange(contentType: ContentType, contentId: Long, action: ModerationActionType) {
        val newVisibility = when (action) {
            ModerationActionType.NO_ACTION -> return // No change needed
            ModerationActionType.SET_LIMITED -> ContentVisibility.LIMITED
            ModerationActionType.SET_HIDDEN -> ContentVisibility.HIDDEN
            ModerationActionType.SET_REMOVED -> ContentVisibility.REMOVED
            ModerationActionType.RESTORE_PUBLIC -> ContentVisibility.PUBLIC
        }

        when (contentType) {
            ContentType.POST -> {
                val post = postRepository.findById(contentId)
                    .orElseThrow { IllegalArgumentException("Post not found: $contentId") }
                postRepository.save(post.copy(visibility = newVisibility))
            }
            ContentType.COMMENT -> {
                val comment = commentRepository.findById(contentId)
                    .orElseThrow { IllegalArgumentException("Comment not found: $contentId") }
                commentRepository.save(comment.copy(visibility = newVisibility))
            }
        }

        logger.info("Visibility changed: $contentType#$contentId -> $newVisibility")
    }

    // ==================== DTO Converters ====================

    private fun Report.toResponseDTO() = ReportResponseDTO(
        id = id,
        reporterId = reporter.id,
        reporterName = reporter.name,
        contentType = contentType,
        contentId = getContentId(),
        reason = reason,
        description = description,
        status = status,
        reviewedById = reviewedBy?.id,
        reviewedByName = reviewedBy?.name,
        reviewedAt = reviewedAt?.let { formatter.format(it) },
        createdAt = formatter.format(createdAt)
    )

    private fun ModerationAction.toResponseDTO() = ModerationActionResponseDTO(
        id = id,
        contentType = contentType,
        contentId = contentId,
        reportId = report?.id,
        actionType = actionType,
        adminId = admin.id,
        adminName = admin.name,
        note = note,
        createdAt = formatter.format(createdAt)
    )
}
