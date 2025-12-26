package com.example.backend.web.repository

import com.example.backend.web.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query


interface CommentRepository : JpaRepository<com.example.backend.web.model.Comment, Long> {
    fun findByPost(post: Post, pageable: Pageable): Page<com.example.backend.web.model.Comment>

    // 최상위 댓글만 조회 (대댓글 제외)
    fun findByPostAndParentCommentIsNull(post: Post, pageable: Pageable): Page<com.example.backend.web.model.Comment>

    // 사용자의 댓글 개수
    fun countByUserId(userId: Long): Long
}

interface BoardRepository : JpaRepository<com.example.backend.web.model.Board, Long> {
    // Find active boards only (for public listing)
    fun findByStatus(status: com.example.backend.web.model.BoardStatus, pageable: Pageable): Page<com.example.backend.web.model.Board>

    // Find boards by type and status
    fun findByBoardTypeAndStatus(
        boardType: com.example.backend.web.model.BoardType,
        status: com.example.backend.web.model.BoardStatus,
        pageable: Pageable
    ): Page<com.example.backend.web.model.Board>

    // Find board by context (e.g., country code)
    fun findByContextAndStatus(
        context: String,
        status: com.example.backend.web.model.BoardStatus
    ): com.example.backend.web.model.Board?
}


interface PostRepository : JpaRepository<Post, Long> {

    fun findByUserId(userId: Long, pageable: Pageable): Page<Post>

    // 사용자의 게시글 개수
    fun countByUserId(userId: Long): Long

    // Board-based queries (primary navigation)
    fun findByBoard(board: com.example.backend.web.model.Board, pageable: Pageable): Page<Post>

    fun countByBoard(board: com.example.backend.web.model.Board): Long

    // Multiple boards (for feed aggregation)
    fun findByBoardIn(boards: List<com.example.backend.web.model.Board>, pageable: Pageable): Page<Post>

    // Search: Keyword in title or content
    @Query("""
        SELECT p FROM Post p
        WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    fun searchByKeyword(keyword: String, pageable: Pageable): Page<Post>

    // Search: Keyword + Boards
    @Query("""
        SELECT p FROM Post p
        WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND p.board IN :boards
    """)
    fun searchByKeywordAndBoards(keyword: String, boards: List<com.example.backend.web.model.Board>, pageable: Pageable): Page<Post>

    // Search: Boards only
    @Query("SELECT p FROM Post p WHERE p.board IN :boards")
    fun searchByBoards(boards: List<com.example.backend.web.model.Board>, pageable: Pageable): Page<Post>

}


interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByCurrentCountry(country: Country): List<User>
    fun findByOriginCountry(country: Country): List<User>
    fun findByOriginCountryAndCurrentCountry(origin: Country, current: Country): List<User>
    fun existsByNickname(nickname: String): Boolean  // For nickname uniqueness check
    fun existsByNicknameKey(nicknameKey: String): Boolean  // For nickname_key uniqueness check

}

interface FollowRepository : JpaRepository<Follow, Long> {
    // Find follow request by requester and target
    fun findByRequesterAndTarget(requester: User, target: User): Follow?

    // Count ACCEPTED follows only
    fun countByRequesterAndStatus(requester: User, status: FollowStatus): Long
    fun countByTargetAndStatus(target: User, status: FollowStatus): Long

    // Find follows by status
    fun findByRequester(requester: User, pageable: Pageable): Page<Follow>
    fun findByTarget(target: User, pageable: Pageable): Page<Follow>
    fun findByRequesterAndStatus(requester: User, status: FollowStatus, pageable: Pageable): Page<Follow>
    fun findByTargetAndStatus(target: User, status: FollowStatus, pageable: Pageable): Page<Follow>

    // Find pending requests (REQUESTED status)
    fun findByTargetAndStatus(target: User, status: FollowStatus): List<Follow>

    // Check for recent rejection (for cooldown)
    @Query("""
        SELECT f FROM Follow f
        WHERE f.requester = :requester
        AND f.target = :target
        AND f.status = 'REJECTED'
        ORDER BY f.respondedAt DESC
    """)
    fun findRecentRejection(requester: User, target: User): Follow?
}

interface LikeRepository : JpaRepository<Like, Long> {
    fun findByUserAndPost(user: User, post: Post): Like?
    fun findByUserAndComment(user: User, comment: com.example.backend.web.model.Comment): Like?
    @Query("SELECT l.user FROM Like l WHERE l.post.id = :postId")
    fun findUsersByPostId(postId: Long, pageable: Pageable): Page<User>
    fun countByPost(post: Post): Long
    fun countByComment(comment: com.example.backend.web.model.Comment): Long
}

interface AuthorizationCodeRepository : JpaRepository<AuthorizationCode, Long> {
    fun findByCode(code: String): AuthorizationCode?
    fun deleteByExpiresAtBefore(expiresAt: java.time.LocalDateTime)
}

/*
interface MessageRepository : JpaRepository<Message, Long> {
    fun findByChatRoom(chatRoom: ChatRoom): List<Message>
}*/

