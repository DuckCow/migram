package com.example.backend.web.controller

import com.example.backend.web.model.FollowStatus
import com.example.backend.web.model.UserDTO
import com.example.backend.web.service.FollowDTO
import com.example.backend.web.service.FollowRequestResult
import com.example.backend.web.service.FollowService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Follow Controller - Request-based Follow System
 *
 * Endpoints:
 * - POST /api/follows/request/{targetId} - Send follow request
 * - POST /api/follows/accept/{requestId} - Accept follow request
 * - POST /api/follows/reject/{requestId} - Reject follow request
 * - DELETE /api/follows/cancel/{targetId} - Cancel sent request
 * - DELETE /api/follows/unfollow/{targetId} - Unfollow
 * - GET /api/follows/requests/pending - Get received requests
 * - GET /api/follows/requests/sent - Get sent requests
 * - GET /api/follows/{userId}/followers - Get followers (ACCEPTED)
 * - GET /api/follows/{userId}/followings - Get followings (ACCEPTED)
 * - GET /api/follows/{userId}/followers/count - Get follower count
 * - GET /api/follows/{userId}/followings/count - Get following count
 * - GET /api/follows/status/{targetId} - Get follow status
 */
@RestController
@RequestMapping("/api/follows")
class FollowController(private val followService: FollowService) {

    /**
     * Send follow request
     * POST /api/follows/request/{targetId}
     *
     * Checks:
     * - Already following
     * - Already requested
     * - Cooldown active
     */
    @PostMapping("/request/{targetId}")
    fun sendFollowRequest(@PathVariable targetId: Long): ResponseEntity<FollowRequestResult> {
        val result = followService.createFollowRequest(targetId)
        val status = if (result.success) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(result)
    }

    /**
     * Accept follow request (target user only)
     * POST /api/follows/accept/{requestId}
     */
    @PostMapping("/accept/{requestId}")
    fun acceptFollowRequest(@PathVariable requestId: Long): ResponseEntity<FollowRequestResult> {
        return try {
            val result = followService.acceptFollowRequest(requestId)
            val status = if (result.success) HttpStatus.OK else HttpStatus.BAD_REQUEST
            ResponseEntity.status(status).body(result)
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(FollowRequestResult(false, e.message ?: "권한이 없습니다"))
        }
    }

    /**
     * Reject follow request (target user only)
     * POST /api/follows/reject/{requestId}
     */
    @PostMapping("/reject/{requestId}")
    fun rejectFollowRequest(@PathVariable requestId: Long): ResponseEntity<FollowRequestResult> {
        return try {
            val result = followService.rejectFollowRequest(requestId)
            val status = if (result.success) HttpStatus.OK else HttpStatus.BAD_REQUEST
            ResponseEntity.status(status).body(result)
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(FollowRequestResult(false, e.message ?: "권한이 없습니다"))
        }
    }

    /**
     * Cancel sent follow request (requester only)
     * DELETE /api/follows/cancel/{targetId}
     */
    @DeleteMapping("/cancel/{targetId}")
    fun cancelFollowRequest(@PathVariable targetId: Long): ResponseEntity<Map<String, Any>> {
        val success = followService.cancelFollowRequest(targetId)
        return ResponseEntity.ok(
            mapOf(
                "success" to success,
                "message" to if (success) "요청을 취소했습니다" else "취소할 요청이 없습니다"
            )
        )
    }

    /**
     * Unfollow (delete ACCEPTED relationship)
     * DELETE /api/follows/unfollow/{targetId}
     */
    @DeleteMapping("/unfollow/{targetId}")
    fun unfollow(@PathVariable targetId: Long): ResponseEntity<Map<String, Any>> {
        val success = followService.unfollow(targetId)
        return ResponseEntity.ok(
            mapOf(
                "success" to success,
                "message" to if (success) "언팔로우했습니다" else "팔로우 관계가 없습니다"
            )
        )
    }

    /**
     * Get received follow requests (REQUESTED status, current user is target)
     * GET /api/follows/requests/pending
     */
    @GetMapping("/requests/pending")
    fun getPendingRequests(): ResponseEntity<List<FollowDTO>> {
        val requests = followService.getPendingRequests()
        return ResponseEntity.ok(requests)
    }

    /**
     * Get sent follow requests (REQUESTED status, current user is requester)
     * GET /api/follows/requests/sent
     */
    @GetMapping("/requests/sent")
    fun getSentRequests(): ResponseEntity<List<FollowDTO>> {
        val requests = followService.getSentRequests()
        return ResponseEntity.ok(requests)
    }

    /**
     * Get followers (ACCEPTED only)
     * GET /api/follows/{userId}/followers
     */
    @GetMapping("/{userId}/followers")
    fun getFollowers(
        @PathVariable userId: Long,
        pageable: Pageable
    ): ResponseEntity<Page<UserDTO>> {
        val followers = followService.getFollowers(userId, pageable)
        return ResponseEntity.ok(followers)
    }

    /**
     * Get followings (ACCEPTED only)
     * GET /api/follows/{userId}/followings
     */
    @GetMapping("/{userId}/followings")
    fun getFollowings(
        @PathVariable userId: Long,
        pageable: Pageable
    ): ResponseEntity<Page<UserDTO>> {
        val followings = followService.getFollowings(userId, pageable)
        return ResponseEntity.ok(followings)
    }

    /**
     * Get follower count (ACCEPTED only)
     * GET /api/follows/{userId}/followers/count
     */
    @GetMapping("/{userId}/followers/count")
    fun getFollowerCount(@PathVariable userId: Long): ResponseEntity<Long> {
        val count = followService.getFollowerCount(userId)
        return ResponseEntity.ok(count)
    }

    /**
     * Get following count (ACCEPTED only)
     * GET /api/follows/{userId}/followings/count
     */
    @GetMapping("/{userId}/followings/count")
    fun getFollowingCount(@PathVariable userId: Long): ResponseEntity<Long> {
        val count = followService.getFollowingCount(userId)
        return ResponseEntity.ok(count)
    }

    /**
     * Get follow status for target user
     * GET /api/follows/status/{targetId}
     *
     * Returns: null (no relationship), REQUESTED, ACCEPTED, REJECTED
     */
    @GetMapping("/status/{targetId}")
    fun getFollowStatus(@PathVariable targetId: Long): ResponseEntity<Map<String, Any?>> {
        val status = followService.getFollowStatusForTarget(targetId)
        return ResponseEntity.ok(
            mapOf(
                "status" to status?.name,
                "isFollowing" to (status == FollowStatus.ACCEPTED)
            )
        )
    }

    /**
     * Check if user A is following user B (ACCEPTED status)
     * GET /api/follows/check?requesterId=1&targetId=2
     */
    @GetMapping("/check")
    fun checkFollowStatus(
        @RequestParam requesterId: Long,
        @RequestParam targetId: Long
    ): ResponseEntity<Boolean> {
        val isFollowing = followService.isFollowing(requesterId, targetId)
        return ResponseEntity.ok(isFollowing)
    }
}
