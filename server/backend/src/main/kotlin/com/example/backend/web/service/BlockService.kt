package com.example.backend.web.service

import com.example.backend.web.model.Block
import com.example.backend.web.model.FollowStatus
import com.example.backend.web.repository.BlockRepository
import com.example.backend.web.repository.FollowRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class BlockResult(
    val success: Boolean,
    val message: String
)

@Service
@Transactional
class BlockService(
    private val blockRepository: BlockRepository,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val securityUtils: SecurityUtils
) {

    /**
     * Block a user
     * - Create block relationship
     * - Unfollow both directions (if exists)
     * - Prevent future follows and DMs
     */
    fun blockUser(targetUserId: Long): BlockResult {
        val currentUserId = securityUtils.getCurrentUserId()

        if (currentUserId == targetUserId) {
            return BlockResult(false, "Cannot block yourself")
        }

        val blocker = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Current user not found") }
        val blocked = userRepository.findById(targetUserId)
            .orElseThrow { EntityNotFoundException("Target user not found") }

        // Check if already blocked
        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            return BlockResult(false, "User is already blocked")
        }

        // Create block
        val block = Block(
            blocker = blocker,
            blocked = blocked
        )
        blockRepository.save(block)

        // Unfollow both directions
        // 1. Blocker -> Blocked (if exists)
        followRepository.findByRequesterAndTarget(blocker, blocked)?.let { follow ->
            if (follow.status == FollowStatus.ACCEPTED || follow.status == FollowStatus.REQUESTED) {
                followRepository.delete(follow)
            }
        }

        // 2. Blocked -> Blocker (if exists)
        followRepository.findByRequesterAndTarget(blocked, blocker)?.let { follow ->
            if (follow.status == FollowStatus.ACCEPTED || follow.status == FollowStatus.REQUESTED) {
                followRepository.delete(follow)
            }
        }

        return BlockResult(true, "User blocked successfully")
    }

    /**
     * Unblock a user
     */
    fun unblockUser(targetUserId: Long): BlockResult {
        val currentUserId = securityUtils.getCurrentUserId()

        val blocker = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Current user not found") }
        val blocked = userRepository.findById(targetUserId)
            .orElseThrow { EntityNotFoundException("Target user not found") }

        val block = blockRepository.findByBlockerAndBlocked(blocker, blocked)
            ?: return BlockResult(false, "User is not blocked")

        blockRepository.delete(block)

        return BlockResult(true, "User unblocked successfully")
    }

    /**
     * Check if blocker has blocked blocked user
     */
    fun isBlocked(blockerId: Long, blockedId: Long): Boolean {
        val blocker = userRepository.findById(blockerId).orElse(null) ?: return false
        val blocked = userRepository.findById(blockedId).orElse(null) ?: return false

        return blockRepository.existsByBlockerAndBlocked(blocker, blocked)
    }

    /**
     * Check if current user has blocked target user
     */
    fun hasBlocked(targetUserId: Long): Boolean {
        val currentUserId = securityUtils.getCurrentUserId()
        return isBlocked(currentUserId, targetUserId)
    }

    /**
     * Check if current user is blocked by target user
     */
    fun isBlockedBy(targetUserId: Long): Boolean {
        val currentUserId = securityUtils.getCurrentUserId()
        return isBlocked(targetUserId, currentUserId)
    }

    /**
     * Check if there's any block between two users (either direction)
     */
    fun hasBlockRelationship(userId1: Long, userId2: Long): Boolean {
        return isBlocked(userId1, userId2) || isBlocked(userId2, userId1)
    }

    /**
     * Get list of blocked users by current user
     */
    fun getBlockedUsers(): List<Long> {
        val currentUserId = securityUtils.getCurrentUserId()
        val blocker = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Current user not found") }

        return blockRepository.findByBlocker(blocker).map { it.blocked.id }
    }
}
