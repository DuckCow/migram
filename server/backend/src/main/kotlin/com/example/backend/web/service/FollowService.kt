package com.example.backend.web.service

import com.example.backend.web.model.Follow
import com.example.backend.web.model.UserDTO
import com.example.backend.web.model.toDTO
import com.example.backend.web.repository.FollowRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val securityUtils: SecurityUtils
) {
    fun follow(followingId: Long): Boolean {
        val currentUserId = securityUtils.getCurrentUserId()
        val follower = userRepository.findById(currentUserId).orElseThrow { EntityNotFoundException("Follower not found") }
        val following = userRepository.findById(followingId).orElseThrow { EntityNotFoundException("Following user not found") }

        if (followRepository.findByFollowerAndFollowing(follower, following) != null) {
            return false // Already following
        }

        val follow = Follow(follower = follower, following = following)
        followRepository.save(follow)
        return true
    }

    fun unfollow(followingId: Long): Boolean {
        val currentUserId = securityUtils.getCurrentUserId()
        val follower = userRepository.findById(currentUserId).orElseThrow { EntityNotFoundException("Follower not found") }
        val following = userRepository.findById(followingId).orElseThrow { EntityNotFoundException("Following user not found") }

        val follow = followRepository.findByFollowerAndFollowing(follower, following) ?: return false
        followRepository.delete(follow)
        return true
    }

    fun getFollowers(userId: Long, pageable: Pageable): Page<UserDTO> {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        return followRepository.findByFollowing(user, pageable).map { it.follower.toDTO() }
    }

    fun getFollowings(userId: Long, pageable: Pageable): Page<UserDTO> {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        return followRepository.findByFollower(user, pageable).map { it.following.toDTO() }
    }

    fun getFollowersCount(userId: Long): Long {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        return followRepository.countByFollowing(user)
    }

    fun getFollowingsCount(userId: Long): Long {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        return followRepository.countByFollower(user)
    }


    fun isFollowing(followerId: Long, followingId: Long): Boolean {
        val follower = userRepository.findById(followerId).orElseThrow { EntityNotFoundException("Follower not found") }
        val following = userRepository.findById(followingId).orElseThrow { EntityNotFoundException("Following user not found") }
        return followRepository.findByFollowerAndFollowing(follower, following) != null
    }
}