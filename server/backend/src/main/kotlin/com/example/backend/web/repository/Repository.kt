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
    // 기본적인 국가별 조회
    fun findByCountry(country: Country): List<Post>

    // 페이징 처리를 포함한 국가별 조회
    fun findByCountry(country: Country, pageable: Pageable): Page<Post>

    // 특정 국가들의 게시물 조회
    fun findByCountryIn(countries: List<Country>): List<Post>

    // 국가별 + 생성일자 정렬
    fun findByCountryOrderByCreatedAtDesc(country: Country): List<Post>

}


interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByCurrentCountry(country: Country): List<User>
    fun findByOriginCountry(country: Country): List<User>
    fun findByOriginCountryAndCurrentCountry(origin: Country, current: Country): List<User>

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

