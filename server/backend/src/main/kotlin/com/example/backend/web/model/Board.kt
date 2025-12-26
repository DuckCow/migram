package com.example.backend.web.model

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

/**
 * Board = Community (게시판)
 * Represents a discussion board/community where posts are aggregated.
 *
 * In MVP: Country/City boards only
 * Future: Topic/Category boards (same concept)
 */
@Entity
@Table(name = "boards")
data class Board(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    val name: String, // e.g., "Korea", "Seoul", "Travel Tips"

    @Column(length = 500)
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val boardType: BoardType = BoardType.COUNTRY, // COUNTRY, CITY, TOPIC

    @Column(length = 100)
    val context: String? = null, // For COUNTRY/CITY: country code, For TOPIC: topic ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: BoardStatus = BoardStatus.PENDING, // ACTIVE, PENDING, REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_created_by")
    val boardCreatedBy: User? = null // Null for system-created boards
) : Auditable()

enum class BoardType {
    COUNTRY,  // Country-based community (MVP)
    CITY,     // City-based community (MVP)
    TOPIC     // Topic-based community (Future)
}

enum class BoardStatus {
    ACTIVE,   // Approved and visible
    PENDING,  // Awaiting admin approval
    REJECTED  // Rejected by admin
}

// DTOs
data class BoardResponseDTO(
    val id: Long,
    val name: String,
    val description: String?,
    val boardType: String,
    val context: String?,
    val status: String,
    val createdBy: UserDTO?,
    val createdAt: String,
    val postCount: Long = 0 // Number of posts in this board
)

data class BoardCreateRequestDTO(
    val name: String,
    val description: String?,
    val boardType: BoardType,
    val context: String? // e.g., country code or city name
)

// Extension function
fun Board.toResponseDTO(postCount: Long = 0): BoardResponseDTO {
    return BoardResponseDTO(
        id = id,
        name = name,
        description = description,
        boardType = boardType.name,
        context = context,
        status = status.name,
        createdBy = boardCreatedBy?.toDTO(),
        createdAt = this.createdAt.toString(),
        postCount = postCount
    )
}
