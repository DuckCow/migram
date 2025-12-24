package com.example.backend.web.model

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

@Entity
@Table(
    name = "post",
    indexes = [
        Index(name = "idx_post_country", columnList = "country"),
        Index(name = "idx_post_title", columnList = "title"),
        Index(name = "idx_post_media_url", columnList = "mediaUrl")
    ]
)
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
    val votes: MutableList<Vote> = mutableListOf()

) : Auditable()

data class PostRequestDTO(
    val title: String,
    val content: String,
    val imageUrl: String?,
    val country: Country  // Required: Country selection when creating post
)

data class PostResponseDTO(
    val id: Long,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val country: Country,
    val userDTO: UserDTO,
    val upvotes: Long,
    val downvotes: Long,
    val netVotes: Long,
    val currentUserVote: VoteType?,
    val commentCount: Long,
    val createdAt: String,
    val createdBy: String,
    val modifiedAt: String,
    val hotScore: Double? = null
)

/**
 * Serializable page wrapper for Redis caching
 */
data class PageDTO<T>(
    val content: List<T> = emptyList(),
    val pageNumber: Int = 0,
    val pageSize: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val isFirst: Boolean = true,
    val isLast: Boolean = true,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

