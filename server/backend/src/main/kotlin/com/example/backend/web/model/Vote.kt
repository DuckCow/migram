package com.example.backend.web.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "votes",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "post_id"]),
        UniqueConstraint(columnNames = ["user_id", "comment_id"])
    ]
)
data class Vote(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    val comment: Comment? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var voteType: VoteType,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        // Ensure exactly one of post or comment is set (XOR)
        require((post != null) xor (comment != null)) {
            "Vote must be associated with either a post or a comment, but not both"
        }
    }
}
