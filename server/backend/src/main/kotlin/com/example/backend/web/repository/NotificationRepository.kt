package com.example.backend.web.repository

import com.example.backend.web.model.Notification
import com.example.backend.web.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByRecipientOrderByCreatedAtDesc(recipient: User, pageable: Pageable): Page<Notification>

    fun findByRecipientAndIsReadOrderByCreatedAtDesc(
        recipient: User,
        isRead: Boolean,
        pageable: Pageable
    ): Page<Notification>

    fun countByRecipientAndIsRead(recipient: User, isRead: Boolean): Long

    fun findByRecipient(recipient: User): List<Notification>
}
