package com.example.backend.web.controller

import com.example.backend.web.model.*
import com.example.backend.web.service.ModerationService
import com.example.backend.web.util.SecurityUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * User-facing report endpoint
 * Any authenticated user can submit reports
 */
@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val moderationService: ModerationService,
    private val securityUtils: SecurityUtils
) {
    /**
     * Submit a report for a post or comment
     * POST /api/reports
     */
    @PostMapping
    fun createReport(@RequestBody request: ReportRequestDTO): ResponseEntity<ReportResponseDTO> {
        val reporterId = securityUtils.getCurrentUserId()
        val report = moderationService.createReport(request, reporterId)
        return ResponseEntity.status(HttpStatus.CREATED).body(report)
    }
}

/**
 * Admin-only moderation endpoints
 * Requires ADMIN role for all operations
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminModerationController(
    private val moderationService: ModerationService,
    private val securityUtils: SecurityUtils
) {
    // ==================== Report Management ====================

    /**
     * Get pending reports for review
     * GET /api/admin/reports?status=PENDING
     */
    @GetMapping("/reports")
    fun getReports(
        @RequestParam(required = false) status: ReportStatus?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<ReportResponseDTO>> {
        val reports = moderationService.getReports(status, pageable)
        return ResponseEntity.ok(reports)
    }

    /**
     * Get single report details
     * GET /api/admin/reports/{id}
     */
    @GetMapping("/reports/{id}")
    fun getReport(@PathVariable id: Long): ResponseEntity<ReportResponseDTO> {
        val report = moderationService.getReport(id)
        return ResponseEntity.ok(report)
    }

    /**
     * Review a report and apply moderation action
     * POST /api/admin/reports/{id}/review
     *
     * Request body:
     * {
     *   "status": "RESOLVED" | "REJECTED",
     *   "action": "NO_ACTION" | "SET_LIMITED" | "SET_HIDDEN" | "SET_REMOVED" | "RESTORE_PUBLIC",
     *   "adminNote": "optional note"
     * }
     */
    @PostMapping("/reports/{id}/review")
    fun reviewReport(
        @PathVariable id: Long,
        @RequestBody request: ReportReviewRequestDTO
    ): ResponseEntity<ReportResponseDTO> {
        val adminId = securityUtils.getCurrentUserId()
        val report = moderationService.reviewReport(id, request, adminId)
        return ResponseEntity.ok(report)
    }

    // ==================== Direct Moderation ====================

    /**
     * Apply moderation action directly (without a report)
     * POST /api/admin/moderate
     *
     * Request body:
     * {
     *   "contentType": "POST" | "COMMENT",
     *   "contentId": 123,
     *   "action": "SET_LIMITED" | "SET_HIDDEN" | "SET_REMOVED" | "RESTORE_PUBLIC",
     *   "note": "optional reason"
     * }
     */
    @PostMapping("/moderate")
    fun moderateContent(@RequestBody request: DirectModerationRequestDTO): ResponseEntity<ModerationActionResponseDTO> {
        val adminId = securityUtils.getCurrentUserId()
        val action = moderationService.moderateContent(
            contentType = request.contentType,
            contentId = request.contentId,
            action = request.action,
            adminId = adminId,
            note = request.note
        )
        return ResponseEntity.ok(action)
    }

    // ==================== Audit Log ====================

    /**
     * Get moderation history for specific content
     * GET /api/admin/history?contentType=POST&contentId=123
     */
    @GetMapping("/history")
    fun getModerationHistory(
        @RequestParam contentType: ContentType,
        @RequestParam contentId: Long
    ): ResponseEntity<List<ModerationActionResponseDTO>> {
        val history = moderationService.getModerationHistory(contentType, contentId)
        return ResponseEntity.ok(history)
    }

    /**
     * Get recent moderation actions (dashboard)
     * GET /api/admin/actions
     */
    @GetMapping("/actions")
    fun getRecentActions(
        @PageableDefault(size = 50, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<ModerationActionResponseDTO>> {
        val actions = moderationService.getRecentActions(pageable)
        return ResponseEntity.ok(actions)
    }

    // ==================== Statistics ====================

    /**
     * Get pending report count (for dashboard badge)
     * GET /api/admin/reports/count
     */
    @GetMapping("/reports/count")
    fun getPendingReportCount(): ResponseEntity<Map<String, Long>> {
        val reports = moderationService.getReports(ReportStatus.PENDING, Pageable.unpaged())
        return ResponseEntity.ok(mapOf("pending" to reports.totalElements))
    }
}

/**
 * Request DTO for direct moderation (without report)
 */
data class DirectModerationRequestDTO(
    val contentType: ContentType,
    val contentId: Long,
    val action: ModerationActionType,
    val note: String? = null
)
