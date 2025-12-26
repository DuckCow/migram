package com.example.backend.web.controller

import com.example.backend.web.model.SearchQuery
import com.example.backend.web.model.SearchResult
import com.example.backend.web.model.SortType
import com.example.backend.web.service.SearchService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

/**
 * Search Controller
 * Endpoint: GET /posts/search
 */
@RestController
@RequestMapping("/posts")
class SearchController(
    private val searchService: SearchService
) {

    /**
     * Search posts with flexible filters
     *
     * Query Parameters:
     * - keyword: Search term (searches title and content)
     * - boardIds: Comma-separated board IDs (e.g., "1,2,3")
     * - sortBy: Sort type (HOT, RECENT, POPULAR, COMMENTED) - default: HOT
     * - dateFrom: Start date (ISO format: 2025-01-01)
     * - dateTo: End date (ISO format: 2025-12-31)
     * - minVotes: Minimum net votes
     * - hasImage: Filter posts with/without images (true/false)
     * - page, size: Pagination
     *
     * Examples:
     * - /posts/search?keyword=hello
     * - /posts/search?keyword=travel&boardIds=1,2&sortBy=POPULAR
     * - /posts/search?boardIds=3&minVotes=10&hasImage=true
     * - /posts/search?dateFrom=2025-01-01&dateTo=2025-12-31
     */
    @GetMapping("/search")
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) boardIds: String?,
        @RequestParam(required = false, defaultValue = "HOT") sortBy: String,
        @RequestParam(required = false) dateFrom: String?,
        @RequestParam(required = false) dateTo: String?,
        @RequestParam(required = false) minVotes: Int?,
        @RequestParam(required = false) hasImage: Boolean?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): SearchResult {
        // Parse boardIds from comma-separated string
        val boardIdList = boardIds?.split(",")
            ?.mapNotNull { id ->
                try {
                    id.trim().toLong()
                } catch (e: Exception) {
                    null  // Skip invalid board IDs
                }
            }

        // Parse sort type
        val sortType = try {
            SortType.valueOf(sortBy.uppercase())
        } catch (e: Exception) {
            SortType.HOT  // Default to HOT if invalid
        }

        // Build search query
        val searchQuery = SearchQuery(
            keyword = keyword?.takeIf { it.isNotBlank() },
            boardIds = boardIdList,
            sortBy = sortType,
            dateFrom = dateFrom,
            dateTo = dateTo,
            minVotes = minVotes,
            hasImage = hasImage
        )

        return searchService.search(searchQuery, pageable)
    }
}
