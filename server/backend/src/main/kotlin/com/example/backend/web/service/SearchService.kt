package com.example.backend.web.service

import com.example.backend.web.model.SearchQuery
import com.example.backend.web.model.SearchResult
import org.springframework.data.domain.Pageable

/**
 * Search Service Interface
 *
 * Strategy Pattern: Allows swapping implementations without changing client code
 * - Phase 1: DatabaseSearchService (LIKE queries)
 * - Phase 2: FullTextSearchService (PostgreSQL ts_vector)
 * - Phase 3: ElasticsearchSearchService (Elasticsearch)
 * - Phase 4: AISearchService (Vector search)
 */
interface SearchService {
    /**
     * Search posts with filters and pagination
     *
     * @param query Search parameters and filters
     * @param pageable Pagination settings
     * @return SearchResult with posts, total count, and applied filters
     */
    fun search(query: SearchQuery, pageable: Pageable): SearchResult
}
