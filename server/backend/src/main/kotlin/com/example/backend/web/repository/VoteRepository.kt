package com.example.backend.web.repository

import com.example.backend.web.model.Comment
import com.example.backend.web.model.Post
import com.example.backend.web.model.User
import com.example.backend.web.model.Vote
import com.example.backend.web.model.VoteType
import org.springframework.data.jpa.repository.JpaRepository

interface VoteRepository : JpaRepository<Vote, Long> {
    fun findByUserAndPost(user: User, post: Post): Vote?
    fun findByUserAndComment(user: User, comment: Comment): Vote?

    fun countByPostAndVoteType(post: Post, voteType: VoteType): Long
    fun countByCommentAndVoteType(comment: Comment, voteType: VoteType): Long

    fun findByPost(post: Post): List<Vote>
    fun findByComment(comment: Comment): List<Vote>

    fun existsByUserAndPost(user: User, post: Post): Boolean
    fun existsByUserAndComment(user: User, comment: Comment): Boolean
}
