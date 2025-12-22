package com.example.backend.web.service

import com.example.backend.web.model.Vote
import com.example.backend.web.model.VoteType
import com.example.backend.web.repository.CommentRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.repository.VoteRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VoteService(
    private val voteRepository: VoteRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val securityUtils: SecurityUtils
) {
    /**
     * Toggle vote on a post
     * If clicking same vote type again -> remove vote (NONE)
     * If clicking different vote type -> update to new type
     * If no existing vote -> create new vote
     */
    fun votePost(postId: Long, voteType: VoteType): VoteResult {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found") }

        val existingVote = voteRepository.findByUserAndPost(user, post)

        return if (existingVote != null) {
            if (existingVote.voteType == voteType) {
                // Same vote type clicked again -> remove vote
                voteRepository.delete(existingVote)
                VoteResult(VoteType.NONE, getPostVoteCounts(postId))
            } else {
                // Different vote type -> update
                existingVote.voteType = voteType
                voteRepository.save(existingVote)
                VoteResult(voteType, getPostVoteCounts(postId))
            }
        } else {
            // No existing vote -> create new
            voteRepository.save(Vote(user = user, post = post, voteType = voteType))
            VoteResult(voteType, getPostVoteCounts(postId))
        }
    }

    /**
     * Toggle vote on a comment
     */
    fun voteComment(commentId: Long, voteType: VoteType): VoteResult {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val comment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found") }

        val existingVote = voteRepository.findByUserAndComment(user, comment)

        return if (existingVote != null) {
            if (existingVote.voteType == voteType) {
                // Same vote type clicked again -> remove vote
                voteRepository.delete(existingVote)
                VoteResult(VoteType.NONE, getCommentVoteCounts(commentId))
            } else {
                // Different vote type -> update
                existingVote.voteType = voteType
                voteRepository.save(existingVote)
                VoteResult(voteType, getCommentVoteCounts(commentId))
            }
        } else {
            // No existing vote -> create new
            voteRepository.save(Vote(user = user, comment = comment, voteType = voteType))
            VoteResult(voteType, getCommentVoteCounts(commentId))
        }
    }

    fun getPostVoteCounts(postId: Long): VoteCounts {
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found") }
        val upvotes = voteRepository.countByPostAndVoteType(post, VoteType.UP)
        val downvotes = voteRepository.countByPostAndVoteType(post, VoteType.DOWN)
        return VoteCounts(upvotes, downvotes, upvotes - downvotes)
    }

    fun getCommentVoteCounts(commentId: Long): VoteCounts {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found") }
        val upvotes = voteRepository.countByCommentAndVoteType(comment, VoteType.UP)
        val downvotes = voteRepository.countByCommentAndVoteType(comment, VoteType.DOWN)
        return VoteCounts(upvotes, downvotes, upvotes - downvotes)
    }

    fun getUserVoteForPost(postId: Long, userId: Long): VoteType? {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found") }
        return voteRepository.findByUserAndPost(user, post)?.voteType
    }

    fun getUserVoteForComment(commentId: Long, userId: Long): VoteType? {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val comment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found") }
        return voteRepository.findByUserAndComment(user, comment)?.voteType
    }
}

data class VoteResult(
    val currentVote: VoteType,  // NONE means vote was removed
    val counts: VoteCounts
)

data class VoteCounts(
    val upvotes: Long,
    val downvotes: Long,
    val netVotes: Long
)
