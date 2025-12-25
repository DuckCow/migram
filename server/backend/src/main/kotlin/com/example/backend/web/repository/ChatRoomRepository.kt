package com.example.backend.web.repository

import com.example.backend.web.model.ChatRoom
import com.example.backend.web.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {

    /**
     * Find chat room between two users (either direction)
     */
    @Query("""
        SELECT cr FROM ChatRoom cr
        WHERE (cr.user1 = :user1 AND cr.user2 = :user2)
           OR (cr.user1 = :user2 AND cr.user2 = :user1)
    """)
    fun findByUsers(@Param("user1") user1: User, @Param("user2") user2: User): ChatRoom?

    /**
     * Find all chat rooms for a user (sorted by last message time)
     * Excludes BLOCKED rooms (Instagram-style: blocked threads are hidden)
     */
    @Query("""
        SELECT cr FROM ChatRoom cr
        WHERE (cr.user1 = :user OR cr.user2 = :user)
          AND cr.status != 'BLOCKED'
        ORDER BY cr.lastMessageAt DESC NULLS LAST, cr.createdAt DESC
    """)
    fun findByUser(@Param("user") user: User, pageable: Pageable): Page<ChatRoom>

    /**
     * Find all chat rooms for a user (unpaged, for counting purposes)
     * Excludes BLOCKED rooms
     */
    @Query("""
        SELECT cr FROM ChatRoom cr
        WHERE (cr.user1 = :user OR cr.user2 = :user)
          AND cr.status != 'BLOCKED'
    """)
    fun findAllByUser(@Param("user") user: User): List<ChatRoom>
}
