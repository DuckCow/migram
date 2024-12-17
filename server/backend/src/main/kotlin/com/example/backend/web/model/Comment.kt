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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: com.example.backend.web.model.Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: com.example.backend.web.model.User,

    @OneToMany(mappedBy = "comment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val likes: MutableList<com.example.backend.web.model.Like> = mutableListOf()

) : com.example.backend.web.model.Auditable()


data class CommentRequestDTO(
    val content: String,
    val postId: Long
)

data class CommentResponseDTO(
    val id: Long,
    val content: String,
    val postId: Long,
    val userDTO: com.example.backend.web.model.UserDTO,
    val createdAt: LocalDateTime,
    val createdBy: String,
    val modifiedAt: LocalDateTime
)



