package com.example.backend.web.controller

import com.example.backend.web.model.*
import com.example.backend.web.service.PostService
import com.example.backend.web.service.VoteService
import com.example.backend.web.service.VoteCounts
import com.example.backend.web.service.VoteResult
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/posts")
class PostController(
    private val postService: PostService,
    private val voteService: VoteService,
    private val securityUtils: SecurityUtils
) {

    /**
     * Get feed with Hot algorithm sorting
     * Query params:
     * - countries: comma-separated country codes (e.g., "JP,CA,KR")
     * - page, size: pagination
     */
    @GetMapping("/feed")
    fun getFeed(
        @RequestParam(required = false) countries: String?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): PageDTO<PostResponseDTO> {
        val countryList = countries?.split(",")
            ?.mapNotNull { code ->
                try {
                    Country.fromCode(code.trim())
                } catch (e: Exception) {
                    null
                }
            }

        return postService.getFeed(countryList, pageable)
    }

    @GetMapping
    fun getAllPosts(pageable: Pageable): Page<PostResponseDTO> = postService.findAll(pageable)

    @GetMapping("/{postId}")
    fun getPostById(@PathVariable postId: Long): PostResponseDTO = postService.findById(postId)

    @PostMapping
    fun createPost(
        @RequestBody postRequestDTO: PostRequestDTO
    ): ResponseEntity<PostResponseDTO> {
        val createdPost = postService.createPost(postRequestDTO, null)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost)
    }

    /**
     * Create post with image upload
     * POST /posts/with-image
     * Content-Type: multipart/form-data
     * Parts: postData (JSON), image (file)
     */
    @PostMapping("/with-image", consumes = ["multipart/form-data"])
    fun createPostWithImage(
        @RequestPart("title") title: String,
        @RequestPart("content") content: String,
        @RequestPart("country") countryCode: String,
        @RequestPart(value = "image", required = false) image: MultipartFile?
    ): ResponseEntity<PostResponseDTO> {
        val country = try {
            Country.fromCode(countryCode)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid country code: $countryCode")
        }

        val postRequestDTO = PostRequestDTO(
            title = title,
            content = content,
            country = country,
            imageUrl = null  // Will be set by service after upload
        )

        val createdPost = postService.createPost(postRequestDTO, image)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost)
    }

    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @RequestBody postRequestDTO: PostRequestDTO
    ): PostResponseDTO {
        val userId = securityUtils.getCurrentUserId()
        return postService.update(postId, postRequestDTO, userId, null)
    }

    /**
     * Update post with optional image upload
     * PUT /posts/{postId}/with-image
     * Content-Type: multipart/form-data
     */
    @PutMapping("/{postId}/with-image", consumes = ["multipart/form-data"])
    fun updatePostWithImage(
        @PathVariable postId: Long,
        @RequestPart("title") title: String,
        @RequestPart("content") content: String,
        @RequestPart("country") countryCode: String,
        @RequestPart(value = "image", required = false) image: MultipartFile?,
        @RequestPart(value = "keepExistingImage", required = false) keepExistingImageStr: String?
    ): PostResponseDTO {
        val keepExistingImage = keepExistingImageStr?.toBoolean() ?: true
        val userId = securityUtils.getCurrentUserId()

        val country = try {
            Country.fromCode(countryCode)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid country code: $countryCode")
        }

        val postRequestDTO = PostRequestDTO(
            title = title,
            content = content,
            country = country,
            imageUrl = null  // Will be handled by service
        )

        return postService.update(postId, postRequestDTO, userId, image, keepExistingImage)
    }

    @DeleteMapping("/{postId}")
    fun deletePost(@PathVariable postId: Long) {
        val userId = securityUtils.getCurrentUserId()
        postService.delete(postId, userId)
    }

    /**
     * Vote on a post
     * POST /posts/{postId}/vote?type=UP
     * POST /posts/{postId}/vote?type=DOWN
     */
    @PostMapping("/{postId}/vote")
    fun votePost(
        @PathVariable postId: Long,
        @RequestParam type: VoteType
    ): ResponseEntity<VoteResult> {
        return try {
            val result = voteService.votePost(postId, type)
            ResponseEntity.ok(result)
        } catch (e: EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get vote counts for a post
     */
    @GetMapping("/{postId}/votes")
    fun getPostVotes(@PathVariable postId: Long): ResponseEntity<VoteCounts> {
        return try {
            val counts = voteService.getPostVoteCounts(postId)
            ResponseEntity.ok(counts)
        } catch (e: EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/country/{country}")
    fun getPostsByCountry(
        @PathVariable country: Country,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<Post> {
        return postService.getPostsByCountryWithPaging(country, page, size)
    }
}
