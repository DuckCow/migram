package com.example.backend.web.controller

import com.example.backend.web.service.BlockResult
import com.example.backend.web.service.BlockService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/blocks")
class BlockController(
    private val blockService: BlockService
) {

    /**
     * Block a user
     */
    @PostMapping("/{userId}")
    fun blockUser(@PathVariable userId: Long): ResponseEntity<BlockResult> {
        val result = blockService.blockUser(userId)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * Unblock a user
     */
    @DeleteMapping("/{userId}")
    fun unblockUser(@PathVariable userId: Long): ResponseEntity<BlockResult> {
        val result = blockService.unblockUser(userId)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * Check if current user has blocked target user
     */
    @GetMapping("/{userId}/status")
    fun getBlockStatus(@PathVariable userId: Long): ResponseEntity<BlockStatusResponse> {
        val hasBlocked = blockService.hasBlocked(userId)
        val isBlockedBy = blockService.isBlockedBy(userId)

        return ResponseEntity.ok(
            BlockStatusResponse(
                hasBlocked = hasBlocked,
                isBlockedBy = isBlockedBy
            )
        )
    }

    /**
     * Get list of blocked user IDs
     */
    @GetMapping
    fun getBlockedUsers(): ResponseEntity<List<Long>> {
        val blockedUserIds = blockService.getBlockedUsers()
        return ResponseEntity.ok(blockedUserIds)
    }
}

data class BlockStatusResponse(
    val hasBlocked: Boolean,
    val isBlockedBy: Boolean
)
