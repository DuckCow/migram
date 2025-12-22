package com.example.backend.web.service

import com.example.backend.web.model.Post
import com.example.backend.web.model.VoteType
import com.example.backend.web.repository.VoteRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.pow

@Service
class HotAlgorithmService {
    /**
     * Reddit-style Hot algorithm
     * Formula: score = (upvotes - downvotes) / ((age_in_hours + 2)^1.5)
     *
     * This gives higher weight to:
     * 1. Posts with more net upvotes
     * 2. Recent posts (time decay factor)
     *
     * The +2 offset prevents division by very small numbers for brand new posts
     * The ^1.5 exponent creates a reasonable decay curve
     */
    fun calculateHotScore(
        upvotes: Long,
        downvotes: Long,
        createdAt: LocalDateTime
    ): Double {
        val netVotes = upvotes - downvotes
        val ageInHours = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now()).toDouble()

        // Add 2 to avoid division by very small numbers for very recent posts
        val denominator = (ageInHours + 2).pow(1.5)

        // Ensure we don't divide by zero
        if (denominator == 0.0) {
            return netVotes.toDouble()
        }

        return netVotes.toDouble() / denominator
    }

    /**
     * Batch calculate hot scores for multiple posts
     * More efficient than calculating one by one
     */
    fun calculateHotScores(posts: List<Post>, voteRepository: VoteRepository): Map<Long, Double> {
        return posts.associate { post ->
            val upvotes = voteRepository.countByPostAndVoteType(post, VoteType.UP)
            val downvotes = voteRepository.countByPostAndVoteType(post, VoteType.DOWN)
            post.id to calculateHotScore(upvotes, downvotes, post.createdAt)
        }
    }
}
