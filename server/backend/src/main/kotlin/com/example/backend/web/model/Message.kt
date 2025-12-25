package com.example.backend.web.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "messages",
    indexes = [
        Index(name = "idx_message_chatroom", columnList = "chat_room_id"),
        Index(name = "idx_message_sender", columnList = "sender_id"),
        Index(name = "idx_message_created", columnList = "created_at")
    ]
)
data class Message(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    val chatRoom: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,

    @Column(nullable = true, length = 2000)
    val content: String? = null,

    @Column(name = "image_url", length = 500)
    val imageUrl: String? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(content != null || imageUrl != null) {
            "Message must have either content or imageUrl"
        }
    }
}

data class MessageDTO(
    val id: Long,
    val chatRoomId: Long,
    val senderId: Long,
    val senderNickname: String?,
    val senderProfilePicture: String?,
    val content: String?,
    val imageUrl: String?,
    val isRead: Boolean,
    val readAt: String?,
    val createdAt: String
)

fun Message.toDTO() = MessageDTO(
    id = id,
    chatRoomId = chatRoom.id,
    senderId = sender.id,
    senderNickname = sender.nickname,
    senderProfilePicture = sender.profilePicture,
    content = content,
    imageUrl = imageUrl,
    isRead = isRead,
    readAt = readAt?.toString(),
    createdAt = createdAt.toString()
)

data class ChatRoomDTO(
    val id: Long,
    val otherUser: UserDTO,
    val lastMessageAt: String?,
    val lastMessagePreview: String?,
    val unreadCount: Int,
    val createdAt: String,
    val status: String,  // PENDING, ACTIVE, IGNORED
    val initiatorId: Long,
    val isPending: Boolean
)
