package com.example.backend.web.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notification_recipient", columnList = "recipient_id"),
        Index(name = "idx_notification_created", columnList = "created_at_timestamp"),
        Index(name = "idx_notification_read", columnList = "is_read")
    ]
)
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    val recipient: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    val actor: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,

    @Column(name = "reference_id")
    val referenceId: Long? = null,  // ID of Post, Comment, or Follow

    @Column(nullable = false, length = 500)
    val message: String,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "created_at_timestamp", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
