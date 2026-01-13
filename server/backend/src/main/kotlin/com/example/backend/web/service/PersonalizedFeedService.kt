package com.example.backend.web.service

import com.example.backend.web.config.CacheConfig
import com.example.backend.web.model.*
import com.example.backend.web.repository.BoardRepository
import com.example.backend.web.repository.FollowRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.repository.VoteRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Personalized Feed Service
 *
 * Generates personalized feeds based on:
 * - Follow relationships (0.5x boost for followed users' posts)
 * - Country affinity (0.3x boost for current country, 0.2x for origin country)
 * - Hot algorithm baseline score
 *
 * Caching Strategy:
 * - Personalized feeds: 5 min TTL
 * - Followed users list: 30 min TTL
 *
 * Performance:
 * - Uses batch DTO conversion (O(1) queries instead of O(N))
 * - Redis caching for feed results
 */
@Service
@Transactional(readOnly = true)
class PersonalizedFeedService(
    private val postRepository: PostRepository,
    private val boardRepository: BoardRepository,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val voteRepository: VoteRepository,
    private val hotAlgorithmService: HotAlgorithmService
) {

    /**
     * Get personalized feed for a user
     *
     * @param userId The user ID to personalize for
     * @param boardIds Optional board filter
     * @param pageable Pagination parameters
     * @return Personalized feed with boosted scores
     */
    @Cacheable(
        value = [CacheConfig.PERSONALIZED_FEED_CACHE],
        key = "#userId + ':' + (#boardIds != null ? #boardIds.toString() : 'all') + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
        condition = "@environment.getProperty('spring.cache.type', 'none') != 'none'"
    )
    fun getPersonalizedFeed(
        userId: Long,
        boardIds: List<Long>?,
        pageable: Pageable
    ): PageDTO<PostResponseDTO> {
        // Get user for country information
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }

        // Fetch posts (unpaged for in-memory sorting with personalization)
        val posts = if (boardIds.isNullOrEmpty()) {
            postRepository.findAll().toList()
        } else {
            val boards = boardIds.mapNotNull { boardRepository.findById(it).orElse(null) }
            postRepository.findByBoardIn(boards, Pageable.unpaged()).content
        }

        // Calculate hot scores for all posts
        val hotScores = hotAlgorithmService.calculateHotScores(posts, voteRepository)

        // Get followed user IDs (cached separately)
        val followedUserIds = getFollowedUserIds(userId)

        // Get user's country board IDs
        val userCountryBoardIds = getUserCountryBoardIds(user)

        // Calculate personalized scores
        val personalizedScores = posts.associate { post ->
            val hotScore = hotScores[post.id] ?: 0.0
            val boostMultiplier = calculateBoostMultiplier(
                post = post,
                followedUserIds = followedUserIds,
                currentCountryBoardIds = userCountryBoardIds.currentCountryBoards,
                originCountryBoardIds = userCountryBoardIds.originCountryBoards
            )
            post.id to (hotScore * boostMultiplier)
        }

        // Sort by personalized score
        val sortedPosts = posts.sortedByDescending { personalizedScores[it.id] ?: 0.0 }

        // Convert to DTOs using batch conversion
        val postDTOs = sortedPosts.toResponseDTOs(voteRepository, userId, userRepository)
            .map { it.copy(hotScore = personalizedScores[it.id]) }

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
     * Calculate boost multiplier for a post
     *
     * Base: 1.0
     * + 0.5 if post author is followed
     * + 0.3 if board matches current country
     * + 0.2 if board matches origin country
     *
     * Max: 2.0 (all boosts apply)
     */
    private fun calculateBoostMultiplier(
        post: Post,
        followedUserIds: Set<Long>,
        currentCountryBoardIds: Set<Long>,
        originCountryBoardIds: Set<Long>
    ): Double {
        var multiplier = 1.0

        // Follow boost
        if (post.user.id in followedUserIds) {
            multiplier += 0.5
        }

        // Current country boost
        if (post.board.id in currentCountryBoardIds) {
            multiplier += 0.3
        }

        // Origin country boost
        if (post.board.id in originCountryBoardIds) {
            multiplier += 0.2
        }

        return multiplier
    }

    /**
     * Get list of user IDs that the given user follows (ACCEPTED status only)
     * Cached for 30 minutes
     */
    @Cacheable(
        value = [CacheConfig.FOLLOWED_USERS_CACHE],
        key = "#userId",
        condition = "@environment.getProperty('spring.cache.type', 'none') != 'none'"
    )
    fun getFollowedUserIds(userId: Long): Set<Long> {
        val user = userRepository.findById(userId).orElse(null) ?: return emptySet()

        // Get all ACCEPTED follow relationships where user is the requester
        val acceptedFollows = followRepository.findByRequesterAndStatus(
            user,
            FollowStatus.ACCEPTED,
            Pageable.unpaged()
        ).content

        return acceptedFollows.map { it.target.id }.toSet()
    }

    /**
     * Get board IDs for user's countries
     * Returns separate sets for current and origin country boards
     */
    private fun getUserCountryBoardIds(user: User): UserCountryBoards {
        val currentCountryBoards = user.currentCountry?.let { country ->
            boardRepository.findByContextAndStatus(country.code, BoardStatus.ACTIVE)?.let {
                setOf(it.id)
            } ?: emptySet()
        } ?: emptySet()

        val originCountryBoards = user.originCountry?.let { country ->
            boardRepository.findByContextAndStatus(country.code, BoardStatus.ACTIVE)?.let {
                setOf(it.id)
            } ?: emptySet()
        } ?: emptySet()

        return UserCountryBoards(
            currentCountryBoards = currentCountryBoards,
            originCountryBoards = originCountryBoards
        )
    }

    /**
     * Helper data class for country board IDs
     */
    private data class UserCountryBoards(
        val currentCountryBoards: Set<Long>,
        val originCountryBoards: Set<Long>
    )
}
