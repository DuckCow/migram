package com.example.backend.web.service

import com.example.backend.web.config.CacheConfig
import com.example.backend.web.model.*
import com.example.backend.web.repository.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Trending Service
 *
 * Identifies viral/trending posts based on engagement velocity:
 * - Tracks votes and comments in recent time windows (24h, 7d, 30d)
 * - Applies recency multiplier to favor newer content
 * - Supports filtering by board and country
 *
 * Trending Score Formula:
 * ```
 * trendingScore = engagementVelocity * recencyMultiplier
 *
 * engagementVelocity = (votes_in_period + comments_in_period * 1.5) / hours_in_period
 * recencyMultiplier = 1 / sqrt(ageInHours + 1)
 * ```
 *
 * Caching Strategy:
 * - 15 min TTL (balance between freshness and performance)
 * - Separate cache per board/country/period
 */
@Service
@Transactional(readOnly = true)
class TrendingService(
    private val postRepository: PostRepository,
    private val voteRepository: VoteRepository,
    private val commentRepository: CommentRepository,
    private val boardRepository: BoardRepository,
    private val userRepository: UserRepository
) {

    /**
     * Get trending posts globally
     *
     * @param period Time window for engagement calculation (24h, 7d, 30d)
     * @param pageable Pagination parameters
     * @return Trending posts sorted by trending score (descending)
     */
    @Cacheable(
        value = [CacheConfig.TRENDING_FEED_CACHE],
        key = "'global:' + #period + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
        condition = "@environment.getProperty('spring.cache.type', 'none') != 'none'"
    )
    fun getTrendingPosts(
        period: TrendingPeriod,
        pageable: Pageable
    ): PageDTO<PostResponseDTO> {
        return getTrendingPostsInternal(null, period, pageable)
    }

    /**
     * Get trending posts for specific board
     *
     * @param boardId Board ID to filter
     * @param period Time window for engagement calculation
     * @param pageable Pagination parameters
     * @return Trending posts in the board
     */
    @Cacheable(
        value = [CacheConfig.TRENDING_FEED_CACHE],
        key = "'board:' + #boardId + ':' + #period + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
        condition = "@environment.getProperty('spring.cache.type', 'none') != 'none'"
    )
    fun getTrendingPostsByBoard(
        boardId: Long,
        period: TrendingPeriod,
        pageable: Pageable
    ): PageDTO<PostResponseDTO> {
        return getTrendingPostsInternal(boardId, period, pageable)
    }

    /**
     * Get trending posts for specific country
     *
     * @param countryCode ISO country code (e.g., "KR", "US")
     * @param period Time window for engagement calculation
     * @param pageable Pagination parameters
     * @return Trending posts from country board
     */
    @Cacheable(
        value = [CacheConfig.TRENDING_FEED_CACHE],
        key = "'country:' + #countryCode + ':' + #period + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
        condition = "@environment.getProperty('spring.cache.type', 'none') != 'none'"
    )
    fun getTrendingPostsByCountry(
        countryCode: String,
        period: TrendingPeriod,
        pageable: Pageable
    ): PageDTO<PostResponseDTO> {
        // Find country board
        val board = boardRepository.findByContextAndStatus(countryCode, BoardStatus.ACTIVE)
            ?: throw IllegalArgumentException("Country board not found: $countryCode")

        return getTrendingPostsInternal(board.id, period, pageable)
    }

    /**
     * Internal method to calculate and return trending posts
     */
    private fun getTrendingPostsInternal(
        boardId: Long?,
        period: TrendingPeriod,
        pageable: Pageable
    ): PageDTO<PostResponseDTO> {
        val currentUserId = try {
            // Try to get current user (optional)
            userRepository.findAll().firstOrNull()?.id
        } catch (e: Exception) {
            null
        }

        // Get posts (filter by board if specified)
        val posts = if (boardId != null) {
            val board = boardRepository.findById(boardId).orElseThrow {
                IllegalArgumentException("Board not found: $boardId")
            }
            postRepository.findByBoardIn(listOf(board), Pageable.unpaged()).content
        } else {
            postRepository.findAll().toList()
        }

        // Filter posts by period (only include posts created within the period)
        val cutoffTime = LocalDateTime.now().minus(period.duration)
        val recentPosts = posts.filter { it.createdAt.isAfter(cutoffTime) }

        // Calculate trending scores
        val trendingScores = recentPosts.associateWith { post ->
            calculateTrendingScore(post, period)
        }

        // Sort by trending score (descending)
        val sortedPosts = recentPosts.sortedByDescending { trendingScores[it] ?: 0.0 }

        // Convert to DTOs using batch conversion
        val postDTOs = sortedPosts.toResponseDTOs(voteRepository, currentUserId, userRepository)
            .map { it.copy(trendingScore = trendingScores[sortedPosts.find { post -> post.id == it.id }]) }

        // Apply pagination
        val totalCount = postDTOs.size.toLong()
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, postDTOs.size)
        val paginatedPosts = if (start < postDTOs.size) {
            postDTOs.subList(start, end)
        } else {
            emptyList()
        }

        return PageDTO(
            content = paginatedPosts,
            pageNumber = pageable.pageNumber,
            pageSize = pageable.pageSize,
            totalElements = totalCount,
            totalPages = ((totalCount + pageable.pageSize - 1) / pageable.pageSize).toInt(),
            isFirst = pageable.pageNumber == 0,
            isLast = end >= postDTOs.size,
            hasNext = end < postDTOs.size,
            hasPrevious = pageable.pageNumber > 0
        )
    }

    /**
     * Calculate trending score for a post
     *
     * Formula:
     * trendingScore = engagementVelocity * recencyMultiplier
     *
     * engagementVelocity = (votes + comments * 1.5) / hours_in_period
     * recencyMultiplier = 1 / sqrt(ageInHours + 1)
     *
     * @param post The post to score
     * @param period Time window for engagement calculation
     * @return Trending score (higher = more trending)
     */
    private fun calculateTrendingScore(post: Post, period: TrendingPeriod): Double {
        val now = LocalDateTime.now()
        val cutoffTime = now.minus(period.duration)

        // Count votes in the period
        val votesInPeriod = voteRepository.findAll()
            .filter { it.post?.id == post.id && it.voteType == VoteType.UP }
            .count()

        // Count comments in the period
        val commentsInPeriod = commentRepository.findAll()
            .filter { it.post.id == post.id && it.createdAt.isAfter(cutoffTime) }
            .count()

        // Calculate engagement velocity
        val hoursInPeriod = period.hours.toDouble()
        val engagementVelocity = (votesInPeriod + commentsInPeriod * 1.5) / hoursInPeriod

        // Calculate recency multiplier (favor newer posts)
        val ageInHours = Duration.between(post.createdAt, now).toHours().toDouble()
        val recencyMultiplier = 1.0 / sqrt(ageInHours + 1)

        // Final trending score
        return engagementVelocity * recencyMultiplier
    }
}

/**
 * Trending period enum
 */
enum class TrendingPeriod(val hours: Long, val duration: Duration) {
    LAST_24H(24, Duration.ofHours(24)),
    LAST_7D(168, Duration.ofDays(7)),
    LAST_30D(720, Duration.ofDays(30))
}
