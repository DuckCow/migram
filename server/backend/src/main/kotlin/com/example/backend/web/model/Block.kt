package com.example.backend.web.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "blocks",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["blocker_id", "blocked_id"])
    ],
    indexes = [
        Index(name = "idx_block_blocker", columnList = "blocker_id"),
        Index(name = "idx_block_blocked", columnList = "blocked_id")
    ]
)
data class Block(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    val blocker: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    val blocked: User,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(blocker.id != blocked.id) {
            "Cannot block yourself"
        }
    }
}
