package com.example.backend.web.controller

import com.example.backend.web.dto.UnreadCountResponse
import com.example.backend.web.service.NotificationDTO
import com.example.backend.web.service.NotificationService
import com.example.backend.web.service.toDTO
import com.example.backend.web.util.SecurityUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val securityUtils: SecurityUtils
) {

    /**
     * SSE endpoint for real-time notifications
     * Client should connect to this endpoint to receive live notifications
     */
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamNotifications(): SseEmitter {
        val currentUserId = securityUtils.getCurrentUserId()
        return notificationService.registerEmitter(currentUserId)
    }

    /**
     * Get all notifications for current user (paginated)
     */
    @GetMapping
    fun getNotifications(pageable: Pageable): ResponseEntity<Page<NotificationDTO>> {
        val notifications = notificationService.getNotifications(pageable)
        return ResponseEntity.ok(notifications.map { it.toDTO() })
    }

    /**
     * Get unread notifications for current user (paginated)
     */
    @GetMapping("/unread")
    fun getUnreadNotifications(pageable: Pageable): ResponseEntity<Page<NotificationDTO>> {
        val notifications = notificationService.getUnreadNotifications(pageable)
        return ResponseEntity.ok(notifications.map { it.toDTO() })
    }

    /**
     * Get count of unread notifications
     */
    @GetMapping("/unread/count")
    fun getUnreadCount(): ResponseEntity<UnreadCountResponse> {
        val count = notificationService.getUnreadCount()
        return ResponseEntity.ok(UnreadCountResponse(count))
    }

    /**
     * Mark a specific notification as read
     */
    @PutMapping("/{id}/read")
    fun markAsRead(@PathVariable id: Long): ResponseEntity<Void> {
        notificationService.markAsRead(id)
        return ResponseEntity.ok().build()
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    fun markAllAsRead(): ResponseEntity<Void> {
        notificationService.markAllAsRead()
        return ResponseEntity.ok().build()
    }
}
