package com.example.backend.web.service

import com.example.backend.web.config.CacheConfig
import com.example.backend.web.model.NotificationType
import com.example.backend.web.model.Vote
import com.example.backend.web.model.VoteType
import com.example.backend.web.repository.CommentRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.repository.VoteRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VoteService(
    private val voteRepository: VoteRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val securityUtils: SecurityUtils,
    private val notificationService: NotificationService
) {
    /**
     * Toggle vote on a post
     * If clicking same vote type again -> remove vote (NONE)
     * If clicking different vote type -> update to new type
     * If no existing vote -> create new vote
     */
    @CacheEvict(
        value = [
            CacheConfig.VOTE_COUNT_CACHE,
            CacheConfig.HOT_SCORE_CACHE,
            CacheConfig.FEED_CACHE,
            CacheConfig.USER_VOTES_CACHE,
            CacheConfig.PERSONALIZED_FEED_CACHE  // Invalidate personalized feeds on vote
        ],
        allEntries = true
    )
    fun votePost(postId: Long, voteType: VoteType): VoteResult {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found") }

        val existingVote = voteRepository.findByUserAndPost(user, post)

        val result = if (existingVote != null) {
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

        // Send notification at vote milestones (only for upvotes)
        if (voteType == VoteType.UP && post.user.id != currentUserId) {
            val netVotes = result.counts.netVotes
            if (isVoteMilestone(netVotes)) {
                notificationService.createNotification(
                    recipientId = post.user.id,
                    actorId = currentUserId,
                    type = NotificationType.VOTE_ON_POST,
                    referenceId = postId,
                    message = "Your post reached $netVotes votes!"
                )
            }
        }

        return result
    }

    /**
     * Toggle vote on a comment
     */
    @CacheEvict(
        value = [CacheConfig.VOTE_COUNT_CACHE, CacheConfig.USER_VOTES_CACHE],
        allEntries = true
    )
    fun voteComment(commentId: Long, voteType: VoteType): VoteResult {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val comment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found") }

        val existingVote = voteRepository.findByUserAndComment(user, comment)

        val result = if (existingVote != null) {
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

        // Send notification at vote milestones (only for upvotes)
        if (voteType == VoteType.UP && comment.user.id != currentUserId) {
            val netVotes = result.counts.netVotes
            if (isVoteMilestone(netVotes)) {
                notificationService.createNotification(
                    recipientId = comment.user.id,
                    actorId = currentUserId,
                    type = NotificationType.VOTE_ON_COMMENT,
                    referenceId = commentId,
                    message = "Your comment reached $netVotes votes!"
                )
            }
        }

        return result
    }

    @Cacheable(value = [CacheConfig.VOTE_COUNT_CACHE], key = "'post-' + #postId")
    fun getPostVoteCounts(postId: Long): VoteCounts {
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found") }
        val upvotes = voteRepository.countByPostAndVoteType(post, VoteType.UP)
        val downvotes = voteRepository.countByPostAndVoteType(post, VoteType.DOWN)
        return VoteCounts(upvotes, downvotes, upvotes - downvotes)
    }

    @Cacheable(value = [CacheConfig.VOTE_COUNT_CACHE], key = "'comment-' + #commentId")
    fun getCommentVoteCounts(commentId: Long): VoteCounts {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found") }
        val upvotes = voteRepository.countByCommentAndVoteType(comment, VoteType.UP)
        val downvotes = voteRepository.countByCommentAndVoteType(comment, VoteType.DOWN)
        return VoteCounts(upvotes, downvotes, upvotes - downvotes)
    }

    @Cacheable(value = [CacheConfig.USER_VOTES_CACHE], key = "'post-' + #postId + '-user-' + #userId")
    fun getUserVoteForPost(postId: Long, userId: Long): VoteType? {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found") }
        return voteRepository.findByUserAndPost(user, post)?.voteType
    }

    @Cacheable(value = [CacheConfig.USER_VOTES_CACHE], key = "'comment-' + #commentId + '-user-' + #userId")
    fun getUserVoteForComment(commentId: Long, userId: Long): VoteType? {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val comment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found") }
        return voteRepository.findByUserAndComment(user, comment)?.voteType
    }

    /**
     * Check if a vote count is at a milestone (10, 50, 100, 500, 1000, etc.)
     * Only notify at these milestones to avoid spam
     */
    private fun isVoteMilestone(netVotes: Long): Boolean {
        return netVotes in setOf(10L, 50L, 100L, 500L, 1000L, 5000L, 10000L)
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
