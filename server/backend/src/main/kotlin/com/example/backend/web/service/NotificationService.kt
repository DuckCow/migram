package com.example.backend.web.service

import com.example.backend.web.model.Notification
import com.example.backend.web.model.NotificationType
import com.example.backend.web.model.User
import com.example.backend.web.repository.NotificationRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@Service
@Transactional
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val securityUtils: SecurityUtils
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()

    companion object {
        private const val SSE_TIMEOUT = 30 * 60 * 1000L  // 30 minutes
    }

    /**
     * Create a new notification and send it via SSE if user is connected
     */
    fun createNotification(
        recipientId: Long,
        actorId: Long,
        type: NotificationType,
        referenceId: Long?,
        message: String
    ): Notification {
        val recipient = userRepository.findById(recipientId)
            .orElseThrow { EntityNotFoundException("Recipient not found") }
        val actor = userRepository.findById(actorId)
            .orElseThrow { EntityNotFoundException("Actor not found") }

        val notification = Notification(
            recipient = recipient,
            actor = actor,
            type = type,
            referenceId = referenceId,
            message = message
        )

        val saved = notificationRepository.save(notification)

        // Send via SSE if user is connected
        sendNotificationToUser(recipientId, saved)

        return saved
    }

    /**
     * Get all notifications for current user
     */
    fun getNotifications(pageable: Pageable): Page<Notification> {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable)
    }

    /**
     * Get unread notifications for current user
     */
    fun getUnreadNotifications(pageable: Pageable): Page<Notification> {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }
        return notificationRepository.findByRecipientAndIsReadOrderByCreatedAtDesc(
            user,
            false,
            pageable
        )
    }

    /**
     * Get count of unread notifications
     */
    fun getUnreadCount(): Long {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }
        return notificationRepository.countByRecipientAndIsRead(user, false)
    }

    /**
     * Mark a notification as read
     */
    fun markAsRead(notificationId: Long) {
        val currentUserId = securityUtils.getCurrentUserId()
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { EntityNotFoundException("Notification not found") }

        if (notification.recipient.id != currentUserId) {
            throw SecurityException("You are not authorized to update this notification")
        }

        notification.isRead = true
        notificationRepository.save(notification)
    }

    /**
     * Mark all notifications as read for current user
     */
    fun markAllAsRead() {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }

        val notifications = notificationRepository.findByRecipient(user)
        notifications.forEach { it.isRead = true }
        notificationRepository.saveAll(notifications)
    }

    /**
     * Register SSE emitter for real-time notifications
     */
    fun registerEmitter(userId: Long): SseEmitter {
        val emitter = SseEmitter(SSE_TIMEOUT)

        emitter.onCompletion {
            logger.info("SSE connection completed for user $userId")
            emitters.remove(userId)
        }

        emitter.onTimeout {
            logger.info("SSE connection timed out for user $userId")
            emitters.remove(userId)
        }

        emitter.onError {
            logger.error("SSE connection error for user $userId", it)
            emitters.remove(userId)
        }

        emitters[userId] = emitter

        // Send initial connection message
        try {
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .data("Connected to notification stream")
            )
        } catch (e: IOException) {
            logger.error("Failed to send initial SSE message", e)
            emitters.remove(userId)
        }

        return emitter
    }

    /**
     * Send notification to user via SSE
     */
    private fun sendNotificationToUser(userId: Long, notification: Notification) {
        val emitter = emitters[userId] ?: return

        try {
            emitter.send(
                SseEmitter.event()
                    .name("notification")
                    .data(notification.toDTO())
            )
        } catch (e: IOException) {
            logger.error("Failed to send notification via SSE to user $userId", e)
            emitters.remove(userId)
        }
    }
}

data class NotificationDTO(
    val id: Long,
    val actorId: Long,
    val actorNickname: String?,
    val actorProfilePicture: String?,
    val type: NotificationType,
    val referenceId: Long?,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)

fun Notification.toDTO() = NotificationDTO(
    id = id,
    actorId = actor.id,
    actorNickname = actor.nickname,
    actorProfilePicture = actor.profilePicture,
    type = type,
    referenceId = referenceId,
    message = message,
    isRead = isRead,
    createdAt = createdAt.toString()
)
