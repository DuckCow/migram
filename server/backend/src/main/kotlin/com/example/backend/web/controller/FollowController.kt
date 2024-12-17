package com.example.backend.web.controller

import com.example.backend.web.model.UserDTO
import com.example.backend.web.service.FollowService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/follows")
class FollowController(private val followService: FollowService) {

    @PostMapping("/{followingId}")
    fun follow(@PathVariable followingId: Long): ResponseEntity<Boolean> {
        val result = followService.follow(followingId)
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/{followingId}")
    fun unfollow(@PathVariable followingId: Long): ResponseEntity<Boolean> {
        val result = followService.unfollow(followingId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{userId}/followers")
    fun getFollowers(@PathVariable userId: Long, pageable: Pageable): ResponseEntity<Page<UserDTO>> {
        val followers = followService.getFollowers(userId, pageable)
        return ResponseEntity.ok(followers)
    }

    @GetMapping("/{userId}/followings")
    fun getFollowings(@PathVariable userId: Long, pageable: Pageable): ResponseEntity<Page<UserDTO>> {
        val followings = followService.getFollowings(userId, pageable)
        return ResponseEntity.ok(followings)
    }

    @GetMapping("/{userId}/followers/count")
    fun getFollowersCount(@PathVariable userId: Long): ResponseEntity<Long> {
        val count = followService.getFollowersCount(userId)
        return ResponseEntity.ok(count)
    }

    @GetMapping("/{userId}/followings/count")
    fun getFollowingsCount(@PathVariable userId: Long): ResponseEntity<Long> {
        val count = followService.getFollowingsCount(userId)
        return ResponseEntity.ok(count)
    }

    @GetMapping("/check")
    fun checkFollowStatus(
        @RequestParam followerId: Long,
        @RequestParam followingId: Long
    ): ResponseEntity<Boolean> {
        val isFollowing = followService.isFollowing(followerId, followingId)
        return ResponseEntity.ok(isFollowing)
    }



}