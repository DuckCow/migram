package com.example.backend.web.service

import com.example.backend.web.model.Like
import com.example.backend.web.model.UserDTO
import com.example.backend.web.model.toDTO
import com.example.backend.web.repository.CommentRepository
import com.example.backend.web.repository.LikeRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LikeService(
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val securityUtils: SecurityUtils
) {
    fun togglePostLike(postId: Long): Boolean {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId).orElseThrow { EntityNotFoundException("User not found") }
        val post = postRepository.findById(postId).orElseThrow { EntityNotFoundException("Post not found") }

        if (post.user.id == currentUserId) {
            throw IllegalStateException("You can't like your own post")
        }

        val existingLike = likeRepository.findByUserAndPost(user, post)
        return if (existingLike != null) {
            likeRepository.delete(existingLike)
            false
        } else {
            likeRepository.save(Like(user = user, post = post))
            true
        }
    }

    fun toggleCommentLike(commentId: Long): Boolean {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId).orElseThrow { EntityNotFoundException("User not found") }
        val comment = commentRepository.findById(commentId).orElseThrow { EntityNotFoundException("Comment not found") }

        if (comment.user.id == currentUserId) {
            throw IllegalStateException("You can't like your own comment")
        }

        val existingLike = likeRepository.findByUserAndComment(user, comment)
        return if (existingLike != null) {
            likeRepository.delete(existingLike)
            false
        } else {
            likeRepository.save(Like(user = user, comment = comment))
            true
        }
    }

    fun getPostLikeUsers(postId: Long, pageable: Pageable): Page<UserDTO> {
        val post = postRepository.findById(postId).orElseThrow { EntityNotFoundException("Post not found") }
        return likeRepository.findUsersByPostId(postId, pageable).map { it.toDTO() }
    }

    fun getPostLikeCount(postId: Long): Long {
        val post = postRepository.findById(postId).orElseThrow { EntityNotFoundException("Post not found") }
        return likeRepository.countByPost(post)
    }

    fun getCommentLikeCount(commentId: Long): Long {
        val comment = commentRepository.findById(commentId).orElseThrow { EntityNotFoundException("Comment not found") }
        return likeRepository.countByComment(comment)
    }


}