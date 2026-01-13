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

    // ==================== Visibility-filtered queries ====================

    // Top-level comments with visibility filter
    @Query("""
        SELECT c FROM Comment c
        WHERE c.post = :post
        AND c.parentComment IS NULL
        AND c.visibility IN :visibilities
        ORDER BY c.createdAt DESC
    """)
    fun findByPostAndParentCommentIsNullAndVisibilityIn(
        post: Post,
        visibilities: List<ContentVisibility>,
        pageable: Pageable
    ): Page<Comment>

    // Replies with visibility filter
    @Query("""
        SELECT c FROM Comment c
        WHERE c.parentComment = :parent
        AND c.visibility IN :visibilities
        ORDER BY c.createdAt ASC
    """)
    fun findByParentCommentAndVisibilityIn(
        parent: Comment,
        visibilities: List<ContentVisibility>
    ): List<Comment>
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

    // ==================== Full-Text Search (PostgreSQL) ====================

    /**
     * Full-text search using PostgreSQL ts_vector and ts_rank
     * Searches both title (weight A) and content (weight B)
     *
     * Note: Uses native SQL because JPA/JPQL doesn't support PostgreSQL FTS functions
     * Only works with PostgreSQL - dev profile (H2) uses LIKE search instead
     *
     * @param tsquery Search term in PostgreSQL tsquery format (e.g., "hello & world")
     * @param pageable Pagination parameters
     * @return Page of posts ranked by relevance
     */
    @Query(
        value = """
            SELECT p.*,
                   ts_rank(setweight(p.title_search, 'A') || setweight(p.content_search, 'B'),
                           to_tsquery('simple', :tsquery)) as rank
            FROM post p
            WHERE (setweight(p.title_search, 'A') || setweight(p.content_search, 'B'))
                  @@ to_tsquery('simple', :tsquery)
            ORDER BY rank DESC, p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM post p
            WHERE (setweight(p.title_search, 'A') || setweight(p.content_search, 'B'))
                  @@ to_tsquery('simple', :tsquery)
        """,
        nativeQuery = true
    )
    fun fullTextSearch(
        @org.springframework.data.repository.query.Param("tsquery") tsquery: String,
        pageable: Pageable
    ): Page<Post>

    /**
     * Full-text search with board filtering
     *
     * @param tsquery Search term in PostgreSQL tsquery format
     * @param boardIds List of board IDs to filter by
     * @param pageable Pagination parameters
     * @return Page of posts ranked by relevance
     */
    @Query(
        value = """
            SELECT p.*,
                   ts_rank(setweight(p.title_search, 'A') || setweight(p.content_search, 'B'),
                           to_tsquery('simple', :tsquery)) as rank
            FROM post p
            WHERE (setweight(p.title_search, 'A') || setweight(p.content_search, 'B'))
                  @@ to_tsquery('simple', :tsquery)
              AND p.board_id IN (:boardIds)
            ORDER BY rank DESC, p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM post p
            WHERE (setweight(p.title_search, 'A') || setweight(p.content_search, 'B'))
                  @@ to_tsquery('simple', :tsquery)
              AND p.board_id IN (:boardIds)
        """,
        nativeQuery = true
    )
    fun fullTextSearchByBoards(
        @org.springframework.data.repository.query.Param("tsquery") tsquery: String,
        @org.springframework.data.repository.query.Param("boardIds") boardIds: List<Long>,
        pageable: Pageable
    ): Page<Post>

    // ==================== Visibility-filtered queries ====================

    // Feed query: PUBLIC first, then LIMITED (de-ranked)
    @Query("""
        SELECT p FROM Post p
        WHERE p.board = :board
        AND p.visibility IN :visibilities
        ORDER BY
            CASE p.visibility
                WHEN 'PUBLIC' THEN 0
                WHEN 'LIMITED' THEN 1
                ELSE 2
            END,
            p.createdAt DESC
    """)
    fun findByBoardAndVisibilityInOrderByVisibilityAndCreatedAt(
        board: Board,
        visibilities: List<ContentVisibility>,
        pageable: Pageable
    ): Page<Post>

    // User's own posts (can see HIDDEN but not others' HIDDEN)
    fun findByUserIdAndVisibilityIn(userId: Long, visibilities: List<ContentVisibility>, pageable: Pageable): Page<Post>

    // Admin: find all posts regardless of visibility
    fun findByBoardOrderByCreatedAtDesc(board: Board, pageable: Pageable): Page<Post>

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

// ==================== Moderation Repositories ====================

interface ReportRepository : JpaRepository<Report, Long> {
    // Find reports by status (for admin queue)
    fun findByStatus(status: ReportStatus, pageable: Pageable): Page<Report>

    // Find reports by content
    fun findByPost(post: Post): List<Report>
    fun findByComment(comment: Comment): List<Report>

    // Find pending reports for specific content
    fun findByPostAndStatus(post: Post, status: ReportStatus): List<Report>
    fun findByCommentAndStatus(comment: Comment, status: ReportStatus): List<Report>

    // Check if user already reported this content
    fun existsByReporterAndPost(reporter: User, post: Post): Boolean
    fun existsByReporterAndComment(reporter: User, comment: Comment): Boolean

    // Count pending reports
    fun countByStatus(status: ReportStatus): Long

    // Find reports by content type and status
    fun findByContentTypeAndStatus(contentType: ContentType, status: ReportStatus, pageable: Pageable): Page<Report>
}

interface ModerationActionRepository : JpaRepository<ModerationAction, Long> {
    // Find actions for specific content (audit trail)
    fun findByContentTypeAndContentIdOrderByCreatedAtDesc(
        contentType: ContentType,
        contentId: Long
    ): List<ModerationAction>

    // Find actions by admin
    fun findByAdminOrderByCreatedAtDesc(admin: User, pageable: Pageable): Page<ModerationAction>

    // Find recent actions
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<ModerationAction>

    // Find actions linked to a report
    fun findByReport(report: Report): ModerationAction?
}

/*
interface MessageRepository : JpaRepository<Message, Long> {
    fun findByChatRoom(chatRoom: ChatRoom): List<Message>
}*/

