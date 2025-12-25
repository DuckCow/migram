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
        country = this.country,
        isAnonymous = this.isAnonymous,
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
    includeReplies: Boolean = true,  // 대댓글 포함 여부
    maxDepth: Int = 10  // 최대 중첩 깊이 (무한 재귀 방지)
): com.example.backend.web.model.CommentResponseDTO {
    val upvotes = voteRepository.countByCommentAndVoteType(this, VoteType.UP)
    val downvotes = voteRepository.countByCommentAndVoteType(this, VoteType.DOWN)

    val currentUserVote = if (currentUserId != null) {
        val user = userRepository.findById(currentUserId).orElse(null)
        user?.let { voteRepository.findByUserAndComment(it, this)?.voteType }
    } else null

    // 대댓글 변환 (재귀적으로 모든 깊이 처리)
    // Include all replies, even deleted ones (they will show "This comment was deleted.")
    val repliesDTO = if (includeReplies && maxDepth > 0) {
        this.replies
            .filter { it.parentComment?.id == this.id }  // 이 댓글의 직접 자식만
            .map { reply ->
                reply.toResponseDTO(voteRepository, currentUserId, userRepository, includeReplies = true, maxDepth = maxDepth - 1)
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