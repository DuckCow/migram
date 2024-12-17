package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.CommentRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) {

    fun findByPost(postId: Long, pageable: Pageable): Page<com.example.backend.web.model.CommentResponseDTO> {
        val post = postRepository.findById(postId).orElseThrow { EntityNotFoundException("Post not found") }
        return commentRepository.findByPost(post, pageable).map { it.toResponseDTO() }
    }

    fun findById(commentId: Long): com.example.backend.web.model.CommentResponseDTO = commentRepository.findById(commentId).orElseThrow { EntityNotFoundException("Comment not found") }.toResponseDTO()

    fun save(commentRequestDTO: com.example.backend.web.model.CommentRequestDTO, userId: Long): com.example.backend.web.model.CommentResponseDTO {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val post = postRepository.findById(commentRequestDTO.postId).orElseThrow { EntityNotFoundException("Post not found") }
        val comment = com.example.backend.web.model.Comment(
            id = 0,
            post = post,
            user = user,
            content = commentRequestDTO.content
        )
        return commentRepository.save(comment).toResponseDTO()
    }

    fun update(commentId: Long, updatedCommentRequestDTO: com.example.backend.web.model.CommentRequestDTO, userId: Long): com.example.backend.web.model.CommentResponseDTO {
        val existingComment = commentRepository.findById(commentId).orElseThrow { EntityNotFoundException("Comment not found") }
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val post = postRepository.findById(updatedCommentRequestDTO.postId).orElseThrow { EntityNotFoundException("Post not found") }
        val updatedComment = existingComment.copy(
            content = updatedCommentRequestDTO.content,
            post = post,
            user = user
        )
        return commentRepository.save(updatedComment).toResponseDTO()
    }

    fun delete(commentId: Long) {
        val comment = commentRepository.findById(commentId).orElseThrow { EntityNotFoundException("Comment not found") }
        commentRepository.delete(comment)
    }
}