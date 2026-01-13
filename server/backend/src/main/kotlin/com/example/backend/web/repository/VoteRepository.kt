package com.example.backend.web.repository

import com.example.backend.web.model.Comment
import com.example.backend.web.model.Post
import com.example.backend.web.model.User
import com.example.backend.web.model.Vote
import com.example.backend.web.model.VoteType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VoteRepository : JpaRepository<Vote, Long> {
    fun findByUserAndPost(user: User, post: Post): Vote?
    fun findByUserAndComment(user: User, comment: Comment): Vote?

    fun countByPostAndVoteType(post: Post, voteType: VoteType): Long
    fun countByCommentAndVoteType(comment: Comment, voteType: VoteType): Long

    fun findByPost(post: Post): List<Vote>
    fun findByComment(comment: Comment): List<Vote>

    fun existsByUserAndPost(user: User, post: Post): Boolean
    fun existsByUserAndComment(user: User, comment: Comment): Boolean

    // ==================== Batch Queries for Performance ====================

    /**
     * Batch count votes for multiple posts (solves N+1 problem)
     * Returns list of [postId, voteType, count]
     *
     * Example result: [[1, UP, 10], [1, DOWN, 2], [2, UP, 5]]
     */
    @Query("""
        SELECT v.post.id, v.voteType, COUNT(v)
        FROM Vote v
        WHERE v.post.id IN :postIds
        GROUP BY v.post.id, v.voteType
    """)
    fun countVotesByPostIds(@Param("postIds") postIds: List<Long>): List<Array<Any>>

    /**
     * Batch find user votes for multiple posts
     * Returns list of [postId, voteType]
     *
     * Example result: [[1, UP], [3, DOWN]]
     */
    @Query("""
        SELECT v.post.id, v.voteType
        FROM Vote v
        WHERE v.user = :user AND v.post.id IN :postIds
    """)
    fun findUserVotesByPostIds(
        @Param("user") user: User,
        @Param("postIds") postIds: List<Long>
    ): List<Array<Any>>
}
