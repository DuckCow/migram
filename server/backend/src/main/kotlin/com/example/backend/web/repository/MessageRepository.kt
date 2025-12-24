package com.example.backend.web.repository

import com.example.backend.web.model.ChatRoom
import com.example.backend.web.model.Message
import com.example.backend.web.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MessageRepository : JpaRepository<Message, Long> {

    /**
     * Find messages in a chat room (paginated, ordered by creation time DESC)
     */
    fun findByChatRoomOrderByCreatedAtDesc(chatRoom: ChatRoom, pageable: Pageable): Page<Message>

    /**
     * Count unread messages in a chat room for a specific user
     * (messages not sent by the user and not read)
     */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.chatRoom = :chatRoom
          AND m.sender != :user
          AND m.isRead = false
    """)
    fun countUnreadMessages(@Param("chatRoom") chatRoom: ChatRoom, @Param("user") user: User): Long

    /**
     * Mark all messages in a chat room as read (except those sent by the user)
     */
    @Modifying
    @Query("""
        UPDATE Message m
        SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP
        WHERE m.chatRoom = :chatRoom
          AND m.sender != :user
          AND m.isRead = false
    """)
    fun markAllAsRead(@Param("chatRoom") chatRoom: ChatRoom, @Param("user") user: User)

    /**
     * Get total unread message count for a user across all chat rooms
     */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.chatRoom IN :chatRooms
          AND m.sender != :user
          AND m.isRead = false
    """)
    fun countTotalUnreadMessages(@Param("chatRooms") chatRooms: List<ChatRoom>, @Param("user") user: User): Long
}
