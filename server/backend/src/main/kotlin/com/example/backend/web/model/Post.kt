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

    @Enumerated(EnumType.STRING)
    val country: Country,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val comments: List<Comment> = mutableListOf(),

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    val likes: MutableList<Like> = mutableListOf()

) : Auditable() {
    constructor(title: String, content: String, mediaUrl: String?, user: User) : this(
        id = 0,
        title = title,
        content = content,
        mediaUrl = mediaUrl,
        country = user.currentCountry ?: throw IllegalArgumentException("User must have a current country"),
        user = user
    )
}

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
    val country: Country,  // country 정보 추가
    val userDTO: UserDTO,
    val createdAt: LocalDateTime,
    val createdBy: String,
    val modifiedAt: LocalDateTime
)

