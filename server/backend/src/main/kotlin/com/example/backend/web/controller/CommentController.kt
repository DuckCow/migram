package com.example.backend.web.controller

import com.example.backend.web.model.CommentRequestDTO
import com.example.backend.web.model.CommentResponseDTO
import com.example.backend.web.model.VoteType
import com.example.backend.web.service.CommentService
import com.example.backend.web.service.VoteCounts
import com.example.backend.web.service.VoteResult
import com.example.backend.web.service.VoteService
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/comments")
class CommentController(
    private val commentService: CommentService,
    private val voteService: VoteService,
    private val securityUtils: SecurityUtils
) {

    @GetMapping("/post/{postId}")
    fun getCommentsByPost(
        @PathVariable postId: Long,
        pageable: Pageable
    ): Page<com.example.backend.web.model.CommentResponseDTO> {
        return commentService.findByPost(postId, pageable)
    }

    @GetMapping("/{commentId}")
    fun getCommentById(@PathVariable commentId: Long): com.example.backend.web.model.CommentResponseDTO {
        return commentService.findById(commentId)
    }

    @PostMapping
    fun createComment(
        @RequestBody commentRequestDTO: com.example.backend.web.model.CommentRequestDTO
    ): com.example.backend.web.model.CommentResponseDTO {
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
        val userId = securityUtils.getCurrentUserId()
        commentService.delete(commentId, userId)
    }

    /**
     * Vote on a comment
     * POST /comments/{commentId}/vote?type=UP
     * POST /comments/{commentId}/vote?type=DOWN
     */
    @PostMapping("/{commentId}/vote")
    fun voteComment(
        @PathVariable commentId: Long,
        @RequestParam type: VoteType
    ): ResponseEntity<VoteResult> {
        return try {
            val result = voteService.voteComment(commentId, type)
            ResponseEntity.ok(result)
        } catch (e: EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get vote counts for a comment
     */
    @GetMapping("/{commentId}/votes")
    fun getCommentVotes(@PathVariable commentId: Long): ResponseEntity<VoteCounts> {
        return try {
            val counts = voteService.getCommentVoteCounts(commentId)
            ResponseEntity.ok(counts)
        } catch (e: EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }
}
