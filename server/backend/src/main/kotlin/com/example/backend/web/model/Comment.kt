package com.example.backend.web.model

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

@Entity
data class Comment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val content: String,

    @Column(name = "original_lang", nullable = false, length = 10)
    val originalLang: String = "en",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: com.example.backend.web.model.Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: com.example.backend.web.model.User,

    // 대댓글 기능: 부모 댓글 참조 (null이면 최상위 댓글)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    val parentComment: Comment? = null,

    // Username of user being replied to (preserved even when parent is deleted)
    @Column(name = "reply_to_username", length = 100)
    val replyToUsername: String? = null,

    // User ID being replied to (preserved even when parent is deleted, for clickable @mentions)
    @Column(name = "reply_to_user_id")
    val replyToUserId: Long? = null,

    // Soft delete flag
    @Column(name = "is_deleted", nullable = false)
    val isDeleted: Boolean = false,

    // 대댓글 목록 (NO cascade delete - children remain when parent is soft-deleted)
    @OneToMany(mappedBy = "parentComment", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val replies: MutableList<Comment> = mutableListOf(),

    @OneToMany(mappedBy = "comment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val votes: MutableList<com.example.backend.web.model.Vote> = mutableListOf()

) : com.example.backend.web.model.Auditable()


data class CommentRequestDTO(
    val content: String,
    val postId: Long,
    val parentCommentId: Long? = null,  // 대댓글인 경우 부모 댓글 ID
    val originalLang: String = "en"
)

data class CommentResponseDTO(
    val id: Long,
    val content: String,
    val originalLang: String,
    val postId: Long,
    val userDTO: com.example.backend.web.model.UserDTO,
    val upvotes: Long,
    val downvotes: Long,
    val netVotes: Long,
    val currentUserVote: com.example.backend.web.model.VoteType?,
    val createdAt: String,
    val createdBy: String,
    val modifiedAt: String,
    val parentCommentId: Long? = null,  // 부모 댓글 ID
    val replyToUsername: String? = null,  // Username being replied to (preserved on delete)
    val replyToUserId: Long? = null,  // User ID being replied to (for clickable @mentions)
    val replies: List<CommentResponseDTO> = emptyList(),  // 대댓글 목록
    val replyCount: Int = 0,  // 대댓글 개수
    val isDeleted: Boolean = false  // Soft delete flag
)



