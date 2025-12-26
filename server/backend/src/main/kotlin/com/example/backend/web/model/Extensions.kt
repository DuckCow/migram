package com.example.backend.web.model

import com.example.backend.web.repository.VoteRepository
import com.example.backend.web.repository.UserRepository
import java.time.LocalDateTime

fun Post.toResponseDTO(
    voteRepository: VoteRepository,
    currentUserId: Long?,
    userRepository: UserRepository
): PostResponseDTO {
    val upvotes = voteRepository.countByPostAndVoteType(this, VoteType.UP)
    val downvotes = voteRepository.countByPostAndVoteType(this, VoteType.DOWN)

    val currentUserVote = if (currentUserId != null) {
        val user = userRepository.findById(currentUserId).orElse(null)
        user?.let { voteRepository.findByUserAndPost(it, this)?.voteType }
    } else null

    return PostResponseDTO(
        id = this.id,
        title = this.title,
        content = this.content,
        imageUrl = this.mediaUrl,
        boardId = this.board.id,
        boardName = this.board.name,
        isAnonymous = this.isAnonymous,
        originalLang = this.originalLang,
        userDTO = if (this.isAnonymous) null else this.user.toDTO(),
        upvotes = upvotes,
        downvotes = downvotes,
        netVotes = upvotes - downvotes,
        currentUserVote = currentUserVote,
        commentCount = this.comments.size.toLong(),
        createdAt = this.createdAt.toString(),
        createdBy = this.createdBy ?: "",
        modifiedAt = (this.modifiedAt ?: LocalDateTime.now()).toString()
    )
}

fun com.example.backend.web.model.Comment.toResponseDTO(
    voteRepository: VoteRepository,
    currentUserId: Long?,
    userRepository: UserRepository,
    includeReplies: Boolean = true  // Only include direct replies (depth=1)
): com.example.backend.web.model.CommentResponseDTO {
    val upvotes = voteRepository.countByCommentAndVoteType(this, VoteType.UP)
    val downvotes = voteRepository.countByCommentAndVoteType(this, VoteType.DOWN)

    val currentUserVote = if (currentUserId != null) {
        val user = userRepository.findById(currentUserId).orElse(null)
        user?.let { voteRepository.findByUserAndComment(it, this)?.voteType }
    } else null

    // ENFORCE DEPTH = 1: Only include direct replies if this is a top-level comment
    // If this is already a reply (has parentComment), never include nested replies
    val isTopLevel = this.parentComment == null
    val repliesDTO = if (includeReplies && isTopLevel) {
        // Only process direct children, and never recursively include THEIR replies
        this.replies
            .filter { it.parentComment?.id == this.id }
            .map { reply ->
                // Critical: Pass includeReplies = false to prevent recursive nesting
                reply.toResponseDTO(voteRepository, currentUserId, userRepository, includeReplies = false)
            }
    } else emptyList()

    // Replace content with "This comment was deleted." if soft-deleted
    val displayContent = if (this.isDeleted) {
        "This comment was deleted."
    } else {
        this.content
    }

    return com.example.backend.web.model.CommentResponseDTO(
        id = this.id,
        content = displayContent,
        originalLang = this.originalLang,
        postId = this.post.id,
        userDTO = this.user.toDTO(),
        upvotes = upvotes,
        downvotes = downvotes,
        netVotes = upvotes - downvotes,
        currentUserVote = currentUserVote,
        createdAt = this.createdAt.toString(),
        createdBy = this.createdBy ?: "",
        modifiedAt = (this.modifiedAt ?: LocalDateTime.now()).toString(),
        parentCommentId = this.parentComment?.id,
        replyToUsername = this.replyToUsername,  // Preserved even when parent is deleted
        replyToUserId = this.replyToUserId,  // Preserved for clickable @mentions
        replies = repliesDTO,
        replyCount = this.replies.size,  // Count all replies including deleted ones
        isDeleted = this.isDeleted
    )
}

fun User.toDTO(): UserDTO {
    return UserDTO(
        id = this.id,
        name = this.name,
        email = this.email,
        profilePicture = this.profilePicture,
        nickname = this.nickname,
        nicknameKey = this.nicknameKey,
        originCountry = this.originCountry,
        currentCountry = this.currentCountry,
        allowDmsFrom = this.allowDmsFrom
    )
}

/**
 * Convert Spring Page to cacheable PageDTO
 */
fun <T> org.springframework.data.domain.Page<T>.toPageDTO(): PageDTO<T> {
    return PageDTO(
        content = this.content,
        pageNumber = this.number,
        pageSize = this.size,
        totalElements = this.totalElements,
        totalPages = this.totalPages,
        isFirst = this.isFirst,
        isLast = this.isLast,
        hasNext = this.hasNext(),
        hasPrevious = this.hasPrevious()
    )
}