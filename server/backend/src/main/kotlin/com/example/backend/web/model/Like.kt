package com.example.backend.web.model

import jakarta.persistence.*
import java.time.LocalDateTime


@Entity
@Table(name = "likes")
data class Like(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    val comment: com.example.backend.web.model.Comment? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)