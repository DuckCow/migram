package com.example.backend.web.repository

import com.example.backend.web.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query


interface CommentRepository : JpaRepository<com.example.backend.web.model.Comment, Long> {
    fun findByPost(post: Post, pageable: Pageable): Page<com.example.backend.web.model.Comment>
}


interface PostRepository : JpaRepository<Post, Long> {

    fun findByUserId(userId: Long, pageable: Pageable): Page<Post>

}


interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}

interface FollowRepository : JpaRepository<Follow, Long> {
    fun findByFollowerAndFollowing(follower: User, following: User): Follow?
    fun countByFollower(follower: User): Long
    fun countByFollowing(following: User): Long
    fun findByFollower(follower: User, pageable: Pageable): Page<Follow>
    fun findByFollowing(following: User, pageable: Pageable): Page<Follow>
}

interface LikeRepository : JpaRepository<Like, Long> {
    fun findByUserAndPost(user: User, post: Post): Like?
    fun findByUserAndComment(user: User, comment: com.example.backend.web.model.Comment): Like?
    @Query("SELECT l.user FROM Like l WHERE l.post.id = :postId")
    fun findUsersByPostId(postId: Long, pageable: Pageable): Page<User>
    fun countByPost(post: Post): Long
    fun countByComment(comment: com.example.backend.web.model.Comment): Long


}
/*


interface MessageRepository : JpaRepository<Message, Long> {
    fun findByChatRoom(chatRoom: ChatRoom): List<Message>
}*/

