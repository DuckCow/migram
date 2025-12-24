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
        userDTO = this.user.toDTO(),
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
    includeReplies: Boolean = true  // 대댓글 포함 여부 (무한 재귀 방지)
): com.example.backend.web.model.CommentResponseDTO {
    val upvotes = voteRepository.countByCommentAndVoteType(this, VoteType.UP)
    val downvotes = voteRepository.countByCommentAndVoteType(this, VoteType.DOWN)

    val currentUserVote = if (currentUserId != null) {
        val user = userRepository.findById(currentUserId).orElse(null)
        user?.let { voteRepository.findByUserAndComment(it, this)?.voteType }
    } else null

    // 대댓글 변환 (재귀적으로 처리, 하지만 1단계만)
    val repliesDTO = if (includeReplies) {
        this.replies
            .filter { it.parentComment?.id == this.id }  // 이 댓글의 직접 자식만
            .map { reply ->
                reply.toResponseDTO(voteRepository, currentUserId, userRepository, includeReplies = false)
            }
    } else emptyList()

    return com.example.backend.web.model.CommentResponseDTO(
        id = this.id,
        content = this.content,
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
        replyCount = this.replies.size
    )
}

fun User.toDTO(): UserDTO {
    return UserDTO(
        id = this.id,
        name = this.name,
        email = this.email,
        profilePicture = this.profilePicture,
        nickname = this.nickname,
        originCountry = this.originCountry,
        currentCountry = this.currentCountry
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