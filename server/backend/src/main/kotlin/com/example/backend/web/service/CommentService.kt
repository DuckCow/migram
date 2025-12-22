package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.CommentRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.repository.VoteRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val voteRepository: VoteRepository,
    private val securityUtils: SecurityUtils
) {

    fun findByPost(postId: Long, pageable: Pageable): Page<com.example.backend.web.model.CommentResponseDTO> {
        val currentUserId = try {
            securityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null
        }

        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found") }

        // 최상위 댓글만 조회 (대댓글은 각 댓글의 replies에 포함됨)
        return commentRepository.findByPostAndParentCommentIsNull(post, pageable).map {
            it.toResponseDTO(voteRepository, currentUserId, userRepository)
        }
    }

    fun findById(commentId: Long): com.example.backend.web.model.CommentResponseDTO {
        val currentUserId = try {
            securityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null
        }

        return commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found") }
            .toResponseDTO(voteRepository, currentUserId, userRepository)
    }

    fun save(commentRequestDTO: com.example.backend.web.model.CommentRequestDTO, userId: Long): com.example.backend.web.model.CommentResponseDTO {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val post = postRepository.findById(commentRequestDTO.postId)
            .orElseThrow { EntityNotFoundException("Post not found") }

        // 대댓글인 경우 부모 댓글 확인
        val parentComment = commentRequestDTO.parentCommentId?.let { parentId ->
            commentRepository.findById(parentId)
                .orElseThrow { EntityNotFoundException("Parent comment not found") }
        }

        val comment = com.example.backend.web.model.Comment(
            id = 0,
            post = post,
            user = user,
            content = commentRequestDTO.content,
            parentComment = parentComment
        )

        return commentRepository.save(comment)
            .toResponseDTO(voteRepository, userId, userRepository)
    }

    fun update(commentId: Long, updatedCommentRequestDTO: com.example.backend.web.model.CommentRequestDTO, userId: Long): com.example.backend.web.model.CommentResponseDTO {
        val existingComment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found") }

        // Authorization check: only comment owner can update
        if (existingComment.user.id != userId) {
            throw SecurityException("You are not authorized to update this comment")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val post = postRepository.findById(updatedCommentRequestDTO.postId)
            .orElseThrow { EntityNotFoundException("Post not found") }

        val updatedComment = existingComment.copy(
            content = updatedCommentRequestDTO.content,
            post = post,
            user = user
        )

        return commentRepository.save(updatedComment)
            .toResponseDTO(voteRepository, userId, userRepository)
    }

    fun delete(commentId: Long, userId: Long) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found") }

        // Authorization check: only comment owner can delete
        if (comment.user.id != userId) {
            throw SecurityException("You are not authorized to delete this comment")
        }

        commentRepository.delete(comment)
    }
}
