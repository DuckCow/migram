package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.CommentRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.repository.VoteRepository
import com.example.backend.web.util.SecurityUtils
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Phase 1 Search Implementation
 * Uses LIKE queries for basic text search
 */
@Service
@Transactional(readOnly = true)
class DatabaseSearchService(
    private val postRepository: PostRepository,
    private val voteRepository: VoteRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val hotAlgorithmService: HotAlgorithmService,
    private val securityUtils: SecurityUtils
) : SearchService {

    override fun search(query: SearchQuery, pageable: Pageable): SearchResult {
        // Get current user for vote information (null if anonymous)
        val currentUserId = try {
            securityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null
        }

        // Step 1: Fetch posts based on keyword and countries
        var posts = when {
            // Keyword + Countries
            !query.keyword.isNullOrBlank() && !query.countries.isNullOrEmpty() -> {
                postRepository.searchByKeywordAndCountries(query.keyword, query.countries)
            }
            // Keyword only
            !query.keyword.isNullOrBlank() -> {
                postRepository.searchByKeyword(query.keyword)
            }
            // Countries only
            !query.countries.isNullOrEmpty() -> {
                postRepository.searchByCountries(query.countries)
            }
            // No filters - return all posts
            else -> {
                postRepository.findAll()
            }
        }

        // Step 2: Apply date range filter
        posts = applyDateFilter(posts, query.dateFrom, query.dateTo)

        // Step 3: Apply vote filter
        posts = applyVoteFilter(posts, query.minVotes)

        // Step 4: Apply media filter
        posts = applyMediaFilter(posts, query.hasImage)

        // Step 5: Sort results
        val sortedPosts = sortPosts(posts, query.sortBy)

        // Step 6: Convert to DTOs
        // Calculate hot scores for all posts at once (more efficient)
        val hotScores = if (query.sortBy == SortType.HOT) {
            hotAlgorithmService.calculateHotScores(sortedPosts, voteRepository)
        } else {
            emptyMap()
        }

        val postDTOs = sortedPosts.map { post ->
            post.toResponseDTO(voteRepository, currentUserId, userRepository)
                .copy(hotScore = hotScores[post.id])
        }

        // Step 7: Apply pagination
        val totalCount = postDTOs.size.toLong()
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, postDTOs.size)
        val paginatedPosts = if (start < postDTOs.size) {
            postDTOs.subList(start, end)
        } else {
            emptyList()
        }

        // Step 8: Build result
        val hasMore = end < postDTOs.size
        val filters = SearchFilters(
            countries = query.countries,
            sortBy = query.sortBy,
            dateRange = formatDateRange(query.dateFrom, query.dateTo),
            minVotes = query.minVotes,
            hasImage = query.hasImage
        )

        return SearchResult(
            posts = paginatedPosts,
            totalCount = totalCount,
            hasMore = hasMore,
            query = query.keyword,
            filters = filters
        )
    }

    private fun applyDateFilter(posts: List<Post>, dateFrom: String?, dateTo: String?): List<Post> {
        if (dateFrom == null && dateTo == null) return posts

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val fromDate = dateFrom?.let { LocalDate.parse(it, formatter).atStartOfDay() }
        val toDate = dateTo?.let { LocalDate.parse(it, formatter).atTime(23, 59, 59) }

        return posts.filter { post ->
            val createdAt = post.createdAt
            (fromDate == null || createdAt >= fromDate) &&
            (toDate == null || createdAt <= toDate)
        }
    }

    private fun applyVoteFilter(posts: List<Post>, minVotes: Int?): List<Post> {
        if (minVotes == null) return posts

        return posts.filter { post ->
            val upvotes = voteRepository.countByPostAndVoteType(post, VoteType.UP)
            val downvotes = voteRepository.countByPostAndVoteType(post, VoteType.DOWN)
            val netVotes = upvotes - downvotes
            netVotes >= minVotes
        }
    }

    private fun applyMediaFilter(posts: List<Post>, hasImage: Boolean?): List<Post> {
        if (hasImage == null) return posts

        return if (hasImage) {
            posts.filter { it.mediaUrl != null }
        } else {
            posts.filter { it.mediaUrl == null }
        }
    }

    private fun sortPosts(posts: List<Post>, sortBy: SortType): List<Post> {
        return when (sortBy) {
            SortType.HOT -> {
                // Sort by hot algorithm
                val hotScores = hotAlgorithmService.calculateHotScores(posts, voteRepository)
                posts.sortedByDescending { hotScores[it.id] ?: 0.0 }
            }
            SortType.RECENT -> {
                // Sort by creation date (newest first)
                posts.sortedByDescending { it.createdAt }
            }
            SortType.POPULAR -> {
                // Sort by net votes (most upvoted first)
                posts.sortedByDescending { post ->
                    val upvotes = voteRepository.countByPostAndVoteType(post, VoteType.UP)
                    val downvotes = voteRepository.countByPostAndVoteType(post, VoteType.DOWN)
                    upvotes - downvotes
                }
            }
            SortType.COMMENTED -> {
                // Sort by comment count (most commented first)
                posts.sortedByDescending { post ->
                    commentRepository.findByPost(post, Pageable.unpaged()).totalElements
                }
            }
        }
    }

    private fun formatDateRange(dateFrom: String?, dateTo: String?): String? {
        return when {
            dateFrom != null && dateTo != null -> "$dateFrom ~ $dateTo"
            dateFrom != null -> "$dateFrom ~"
            dateTo != null -> "~ $dateTo"
            else -> null
        }
    }
}
