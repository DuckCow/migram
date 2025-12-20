package com.example.backend.web.controller

import com.example.backend.web.model.CommentRequestDTO
import com.example.backend.web.model.CommentResponseDTO
import com.example.backend.web.service.CommentService
import com.example.backend.web.service.PostService
import com.example.backend.web.service.UserService
import com.example.backend.web.util.SecurityUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.bind.annotation.*
import java.security.Principal



@RestController
@RequestMapping("/comments")
class CommentController(
    private val commentService: CommentService,
    private val securityUtils: SecurityUtils
) {

    @GetMapping("/post/{postId}")
    fun getCommentsByPost(@PathVariable postId: Long, pageable: Pageable): Page<com.example.backend.web.model.CommentResponseDTO> {
        return commentService.findByPost(postId, pageable)
    }

    @GetMapping("/{commentId}")
    fun getCommentById(@PathVariable commentId: Long): com.example.backend.web.model.CommentResponseDTO {
        return commentService.findById(commentId)
    }

    @PostMapping
    fun createComment(@RequestBody commentRequestDTO: com.example.backend.web.model.CommentRequestDTO): com.example.backend.web.model.CommentResponseDTO {
        val userId = securityUtils.getCurrentUserId()
        return commentService.save(commentRequestDTO, userId)
    }

    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody commentRequestDTO: com.example.backend.web.model.CommentRequestDTO
    ): com.example.backend.web.model.CommentResponseDTO {
        val userId = securityUtils.getCurrentUserId()
        return commentService.update(commentId, commentRequestDTO, userId)
    }

    @DeleteMapping("/{commentId}")
    fun deleteComment(@PathVariable commentId: Long) {
        commentService.delete(commentId)
    }
}