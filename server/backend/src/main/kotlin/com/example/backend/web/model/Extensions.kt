package com.example.backend.web.model

import com.example.backend.web.repository.VoteRepository
import com.example.backend.web.repository.UserRepository
import java.time.LocalDateTime

/**
 * Batch convert posts to DTOs (solves N+1 problem)
 *
 * Instead of N queries for vote counts (2 per post), this makes:
 * - 1 query for all vote counts
 * - 1 query for current user's votes (if logged in)
 *
 * Performance: O(N) queries → O(1) queries
 */
fun List<Post>.toResponseDTOs(
    voteRepository: VoteRepository,
    currentUserId: Long?,
    userRepository: UserRepository
): List<PostResponseDTO> {
    if (this.isEmpty()) return emptyList()

    val postIds = this.map { it.id }

    // Single query for all vote counts
    // Result format: [[postId, voteType, count], ...]
    val voteCounts = voteRepository.countVotesByPostIds(postIds)
        .groupBy { it[0] as Long }
        .mapValues { (_, rows) ->
            val upvotes = rows.find { it[1] == VoteType.UP }?.get(2) as? Long ?: 0L
            val downvotes = rows.find { it[1] == VoteType.DOWN }?.get(2) as? Long ?: 0L
            upvotes to downvotes
        }

    // Single query for current user's votes (if logged in)
    // Result format: [[postId, voteType], ...]
    val userVotes = if (currentUserId != null) {
        val user = userRepository.findById(currentUserId).orElse(null)
        user?.let {
            voteRepository.findUserVotesByPostIds(it, postIds)
                .associate { row -> (row[0] as Long) to (row[1] as VoteType) }
        } ?: emptyMap()
    } else emptyMap()

    // Convert to DTOs with pre-fetched data
    return this.map { post ->
        val (upvotes, downvotes) = voteCounts[post.id] ?: (0L to 0L)
        PostResponseDTO(
            id = post.id,
            title = post.title,
            content = post.content,
            imageUrl = post.mediaUrl,
            boardId = post.board.id,
            boardName = post.board.name,
            isAnonymous = post.isAnonymous,
            originalLang = post.originalLang,
            userDTO = if (post.isAnonymous) null else post.user.toDTO(),
            upvotes = upvotes,
            downvotes = downvotes,
            netVotes = upvotes - downvotes,
            currentUserVote = userVotes[post.id],
            commentCount = post.comments.size.toLong(),
            createdAt = post.createdAt.toString(),
            createdBy = post.createdBy ?: "",
            modifiedAt = (post.modifiedAt ?: LocalDateTime.now()).toString()
        )
    }
}

/**
 * Single post conversion (N+1 problem - use batch version when possible)
 *
 * @deprecated Use List<Post>.toResponseDTOs() for multiple posts
 */
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