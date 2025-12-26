package com.example.backend.web.model

import org.springframework.data.domain.Pageable

/**
 * Search query parameters
 * Designed to be extensible for future enhancements (Phase 2, 3)
 */
data class SearchQuery(
    // Phase 1: Basic search
    val keyword: String? = null,           // Search term for title/content
    val boardIds: List<Long>? = null,      // Filter by boards
    val sortBy: SortType = SortType.HOT,   // Default to Hot algorithm

    // Phase 1: Date filtering (optional)
    val dateFrom: String? = null,          // ISO date format: 2025-01-01
    val dateTo: String? = null,            // ISO date format: 2025-12-31

    // Phase 1: Vote filtering (optional)
    val minVotes: Int? = null,             // Minimum net votes

    // Phase 1: Media filtering (optional)
    val hasImage: Boolean? = null,         // Filter posts with/without images

    // Future Phase 2 extensions (prepared but not implemented)
    val tags: List<String>? = null,        // Tag-based search
    val authorId: Long? = null,            // Search by author

    // Future Phase 3 extensions (prepared but not implemented)
    val similarTo: Long? = null,           // Find similar posts to this ID
    val fuzzyMatch: Boolean = false        // Enable fuzzy/approximate matching
) {
    fun hasFilters(): Boolean {
        return keyword != null ||
               !boardIds.isNullOrEmpty() ||
               dateFrom != null ||
               dateTo != null ||
               minVotes != null ||
               hasImage != null ||
               !tags.isNullOrEmpty() ||
               authorId != null ||
               similarTo != null
    }
}

/**
 * Sort options for search results
 */
enum class SortType {
    HOT,           // Hot algorithm (upvotes with time decay)
    RECENT,        // Most recent first
    POPULAR,       // Most upvotes
    COMMENTED      // Most commented
}

/**
 * Search result wrapper
 */
data class SearchResult(
    val posts: List<PostResponseDTO>,
    val totalCount: Long,
    val hasMore: Boolean,
    val query: String?,
    val filters: SearchFilters?
)

/**
 * Applied filters information
 */
data class SearchFilters(
    val boardIds: List<Long>?,
    val sortBy: SortType,
    val dateRange: String?,
    val minVotes: Int?,
    val hasImage: Boolean?
)
