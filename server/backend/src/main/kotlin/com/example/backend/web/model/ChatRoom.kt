package com.example.backend.web.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "chat_rooms",
    indexes = [
        Index(name = "idx_chatroom_user1", columnList = "user1_id"),
        Index(name = "idx_chatroom_user2", columnList = "user2_id"),
        Index(name = "idx_chatroom_updated", columnList = "last_message_at")
    ]
)
data class ChatRoom(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    val user1: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    val user2: User,

    @Column(name = "last_message_at")
    var lastMessageAt: LocalDateTime? = null,

    @Column(name = "last_message_preview", length = 200)
    var lastMessagePreview: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(user1.id != user2.id) {
            "Cannot create chat room with yourself"
        }
    }

    fun getOtherUser(currentUserId: Long): User {
        return if (user1.id == currentUserId) user2 else user1
    }

    fun containsUser(userId: Long): Boolean {
        return user1.id == userId || user2.id == userId
    }
}
