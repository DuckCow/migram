package com.example.backend.web.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Follow Request Status
 * - REQUESTED: 요청 생성됨
 * - ACCEPTED: 수락되어 팔로우 성립
 * - REJECTED: 거부됨 (쿨다운 판단 기준으로 기록 유지)
 */
enum class FollowStatus {
    REQUESTED,
    ACCEPTED,
    REJECTED
}

/**
 * Follow entity (request-based follow system)
 *
 * Rules:
 * - Only ACCEPTED counts as followers/following
 * - REJECTED has cooldown period before re-requesting
 * - REQUESTED can be canceled by requester
 */
@Entity
@Table(
    name = "follow",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["requester_id", "target_id"])
    ],
    indexes = [
        Index(name = "idx_follow_requester", columnList = "requester_id"),
        Index(name = "idx_follow_target", columnList = "target_id"),
        Index(name = "idx_follow_status", columnList = "status")
    ]
)
data class Follow(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    val requester: User,  // 요청을 보낸 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    val target: User,     // 요청을 받은 사용자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FollowStatus = FollowStatus.REQUESTED,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var respondedAt: LocalDateTime? = null  // 수락/거부 시각
) {
    companion object {
        const val COOLDOWN_DAYS = 7L  // 거부 후 재요청 쿨다운 (일)
    }

    /**
     * Check if cooldown period has passed after rejection
     */
    fun isCooldownExpired(): Boolean {
        if (status != FollowStatus.REJECTED || respondedAt == null) {
            return true
        }
        val cooldownEnd = respondedAt!!.plusDays(COOLDOWN_DAYS)
        return LocalDateTime.now().isAfter(cooldownEnd)
    }
}