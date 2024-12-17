package com.example.backend.web.model

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

@Entity
data class Post(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val title: String,
    val content: String,
    val mediaUrl: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val comments: List<com.example.backend.web.model.Comment> = mutableListOf(),

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    val likes: MutableList<Like> = mutableListOf()

) : com.example.backend.web.model.Auditable()


data class PostRequestDTO(
    val title: String,
    val content: String,
    val imageUrl: String?
)

data class PostResponseDTO(
    val id: Long,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val userDTO: UserDTO,
    val createdAt: LocalDateTime,
    val createdBy: String,
    val modifiedAt: LocalDateTime
)