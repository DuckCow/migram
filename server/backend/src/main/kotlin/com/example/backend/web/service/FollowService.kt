package com.example.backend.web.service

import com.example.backend.web.config.CacheConfig
import com.example.backend.web.model.Follow
import com.example.backend.web.model.FollowStatus
import com.example.backend.web.model.NotificationType
import com.example.backend.web.model.UserDTO
import com.example.backend.web.model.toDTO
import com.example.backend.web.repository.FollowRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.cache.annotation.CacheEvict
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class FollowRequestResult(
    val success: Boolean,
    val message: String,
    val status: FollowStatus? = null
)

data class FollowDTO(
    val id: Long,
    val requester: UserDTO,
    val target: UserDTO,
    val status: FollowStatus,
    val createdAt: LocalDateTime,
    val respondedAt: LocalDateTime?
)

fun Follow.toDTO(): FollowDTO = FollowDTO(
    id = this.id,
    requester = this.requester.toDTO(),
    target = this.target.toDTO(),
    status = this.status,
    createdAt = this.createdAt,
    respondedAt = this.respondedAt
)

/**
 * Request-based Follow Service
 *
 * Follow flow:
 * 1. Requester sends follow request → REQUESTED
 * 2. Target can ACCEPT or REJECT
 * 3. Only ACCEPTED counts as followers/following
 * 4. REJECTED has N-day cooldown before re-request
 */
@Service
@Transactional
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val securityUtils: SecurityUtils,
    private val notificationService: NotificationService,
    private val blockService: BlockService
) {

    /**
     * Create follow request
     * Checks:
     * 1. Already ACCEPTED → "Already following"
     * 2. Already REQUESTED → "Already requested"
     * 3. Recently REJECTED + cooldown active → "Cooldown active"
     * 4. Else → Create REQUESTED
     */
    fun createFollowRequest(targetUserId: Long): FollowRequestResult {
        val currentUserId = securityUtils.getCurrentUserId()

        if (currentUserId == targetUserId) {
            return FollowRequestResult(false, "자기 자신을 팔로우할 수 없습니다")
        }

        val requester = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Requester not found") }
        val target = userRepository.findById(targetUserId)
            .orElseThrow { EntityNotFoundException("Target user not found") }

        // Check if blocked or blocking
        if (blockService.hasBlockRelationship(currentUserId, targetUserId)) {
            return FollowRequestResult(false, "Cannot follow this user")
        }

        // Check existing follow relationship
        val existing = followRepository.findByRequesterAndTarget(requester, target)

        when {
            existing != null && existing.status == FollowStatus.ACCEPTED -> {
                return FollowRequestResult(false, "이미 팔로우 중입니다", FollowStatus.ACCEPTED)
            }
            existing != null && existing.status == FollowStatus.REQUESTED -> {
                return FollowRequestResult(false, "이미 요청을 보냈습니다", FollowStatus.REQUESTED)
            }
            existing != null && existing.status == FollowStatus.REJECTED -> {
                // Check cooldown
                if (!existing.isCooldownExpired()) {
                    val daysLeft = Follow.COOLDOWN_DAYS -
                        java.time.temporal.ChronoUnit.DAYS.between(
                            existing.respondedAt!!,
                            LocalDateTime.now()
                        )
                    return FollowRequestResult(
                        false,
                        "거부 후 ${daysLeft}일 후에 다시 요청할 수 있습니다",
                        FollowStatus.REJECTED
                    )
                }
                // Cooldown expired → delete old rejection and create new request
                followRepository.delete(existing)
            }
        }

        // Create new follow request
        val follow = Follow(
            requester = requester,
            target = target,
            status = FollowStatus.REQUESTED
        )
        val saved = followRepository.save(follow)

        // Send notification to target user
        notificationService.createNotification(
            recipientId = targetUserId,
            actorId = currentUserId,
            type = NotificationType.FOLLOW_REQUEST,
            referenceId = saved.id,
            message = "${requester.nickname ?: requester.name} sent you a follow request"
        )

        return FollowRequestResult(true, "팔로우 요청을 보냈습니다", FollowStatus.REQUESTED)
    }

    /**
     * Accept follow request (by target user)
     * Invalidates caches: followed users list and personalized feeds
     */
    @CacheEvict(
        value = [
            CacheConfig.FOLLOWED_USERS_CACHE,
            CacheConfig.PERSONALIZED_FEED_CACHE
        ],
        allEntries = true
    )
    fun acceptFollowRequest(requestId: Long): FollowRequestResult {
        val currentUserId = securityUtils.getCurrentUserId()
        val follow = followRepository.findById(requestId)
            .orElseThrow { EntityNotFoundException("Follow request not found") }

        // Only target can accept
        if (follow.target.id != currentUserId) {
            throw SecurityException("Only the target user can accept this request")
        }

        if (follow.status != FollowStatus.REQUESTED) {
            return FollowRequestResult(false, "이미 처리된 요청입니다", follow.status)
        }

        follow.status = FollowStatus.ACCEPTED
        follow.respondedAt = LocalDateTime.now()
        followRepository.save(follow)

        // Send notification to requester
        notificationService.createNotification(
            recipientId = follow.requester.id,
            actorId = currentUserId,
            type = NotificationType.FOLLOW_ACCEPTED,
            referenceId = follow.id,
            message = "${follow.target.nickname ?: follow.target.name} accepted your follow request"
        )

        return FollowRequestResult(true, "팔로우 요청을 수락했습니다", FollowStatus.ACCEPTED)
    }

    /**
     * Reject follow request (by target user)
     */
    fun rejectFollowRequest(requestId: Long): FollowRequestResult {
        val currentUserId = securityUtils.getCurrentUserId()
        val follow = followRepository.findById(requestId)
            .orElseThrow { EntityNotFoundException("Follow request not found") }

        // Only target can reject
        if (follow.target.id != currentUserId) {
            throw SecurityException("Only the target user can reject this request")
        }

        if (follow.status != FollowStatus.REQUESTED) {
            return FollowRequestResult(false, "이미 처리된 요청입니다", follow.status)
        }

        follow.status = FollowStatus.REJECTED
        follow.respondedAt = LocalDateTime.now()
        followRepository.save(follow)

        return FollowRequestResult(true, "팔로우 요청을 거부했습니다", FollowStatus.REJECTED)
    }

    /**
     * Cancel follow request (by requester)
     * Can cancel REQUESTED status only
     */
    fun cancelFollowRequest(targetUserId: Long): Boolean {
        val currentUserId = securityUtils.getCurrentUserId()
        val requester = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Requester not found") }
        val target = userRepository.findById(targetUserId)
            .orElseThrow { EntityNotFoundException("Target user not found") }

        val follow = followRepository.findByRequesterAndTarget(requester, target)
            ?: return false

        if (follow.status != FollowStatus.REQUESTED) {
            return false
        }

        followRepository.delete(follow)
        return true
    }

    /**
     * Unfollow (delete ACCEPTED relationship)
     * Invalidates caches: followed users list and personalized feeds
     */
    @CacheEvict(
        value = [
            CacheConfig.FOLLOWED_USERS_CACHE,
            CacheConfig.PERSONALIZED_FEED_CACHE
        ],
        allEntries = true
    )
    fun unfollow(targetUserId: Long): Boolean {
        val currentUserId = securityUtils.getCurrentUserId()
        val requester = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Requester not found") }
        val target = userRepository.findById(targetUserId)
            .orElseThrow { EntityNotFoundException("Target user not found") }

        val follow = followRepository.findByRequesterAndTarget(requester, target)
            ?: return false

        if (follow.status != FollowStatus.ACCEPTED) {
            return false
        }

        followRepository.delete(follow)
        return true
    }

    /**
     * Get pending follow requests (받은 요청 목록)
     * Returns REQUESTED follows where current user is target
     */
    fun getPendingRequests(): List<FollowDTO> {
        val currentUserId = securityUtils.getCurrentUserId()
        val target = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }

        return followRepository.findByTargetAndStatus(target, FollowStatus.REQUESTED)
            .map { it.toDTO() }
    }

    /**
     * Get sent requests (보낸 요청 목록)
     * Returns REQUESTED follows where current user is requester
     */
    fun getSentRequests(): List<FollowDTO> {
        val currentUserId = securityUtils.getCurrentUserId()
        val requester = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }

        val follows = followRepository.findByRequesterAndStatus(
            requester,
            FollowStatus.REQUESTED,
            Pageable.unpaged()
        ).content

        return follows.map { it.toDTO() }
    }

    /**
     * Get followers (ACCEPTED only)
     */
    fun getFollowers(userId: Long, pageable: Pageable): Page<UserDTO> {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        return followRepository.findByTargetAndStatus(user, FollowStatus.ACCEPTED, pageable)
            .map { it.requester.toDTO() }
    }

    /**
     * Get followings (ACCEPTED only)
     */
    fun getFollowings(userId: Long, pageable: Pageable): Page<UserDTO> {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        return followRepository.findByRequesterAndStatus(user, FollowStatus.ACCEPTED, pageable)
            .map { it.target.toDTO() }
    }

    /**
     * Get follower count (ACCEPTED only)
     */
    fun getFollowerCount(userId: Long): Long {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        return followRepository.countByTargetAndStatus(user, FollowStatus.ACCEPTED)
    }

    /**
     * Get following count (ACCEPTED only)
     */
    fun getFollowingCount(userId: Long): Long {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        return followRepository.countByRequesterAndStatus(user, FollowStatus.ACCEPTED)
    }

    /**
     * Get follow status between two users
     * Returns: null (no relationship), REQUESTED, ACCEPTED, REJECTED
     */
    fun getFollowStatus(requesterId: Long, targetId: Long): FollowStatus? {
        val requester = userRepository.findById(requesterId)
            .orElseThrow { EntityNotFoundException("Requester not found") }
        val target = userRepository.findById(targetId)
            .orElseThrow { EntityNotFoundException("Target not found") }

        return followRepository.findByRequesterAndTarget(requester, target)?.status
    }

    /**
     * Get follow status for target (using current user as requester)
     */
    fun getFollowStatusForTarget(targetId: Long): FollowStatus? {
        val currentUserId = securityUtils.getCurrentUserId()
        return getFollowStatus(currentUserId, targetId)
    }

    /**
     * Check if user A is following user B (ACCEPTED status)
     */
    fun isFollowing(followerId: Long, followingId: Long): Boolean {
        return getFollowStatus(followerId, followingId) == FollowStatus.ACCEPTED
    }
}
