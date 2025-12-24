package com.example.backend.web.service

import com.example.backend.web.config.CacheConfig
import com.example.backend.web.model.*
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.repository.VoteRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
@Transactional
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val voteRepository: VoteRepository,
    private val securityUtils: SecurityUtils,
    private val s3Service: S3Service,
    private val hotAlgorithmService: HotAlgorithmService
) {

    /**
     * Get feed with Hot algorithm sorting
     * Optionally filter by multiple countries
     *
     * Cached for 10 minutes to reduce computation using PageDTO
     */
    @Cacheable(
        value = [CacheConfig.FEED_CACHE],
        key = "(#countries != null ? #countries.toString() : 'all') + '-page-' + #pageable.pageNumber + '-size-' + #pageable.pageSize"
    )
    fun getFeed(countries: List<Country>?, pageable: Pageable): PageDTO<PostResponseDTO> {
        val currentUserId = try {
            securityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null  // Anonymous user
        }

        // Get posts (filtered by countries if provided)
        val posts = if (countries.isNullOrEmpty()) {
            postRepository.findAll()
        } else {
            postRepository.findByCountryIn(countries)
        }

        // Calculate hot scores
        val hotScores = hotAlgorithmService.calculateHotScores(posts, voteRepository)

        // Sort by hot score descending
        val sortedPosts = posts.sortedByDescending { hotScores[it.id] ?: 0.0 }

        // Convert to DTOs with vote information
        val postDTOs = sortedPosts.map { post ->
            post.toResponseDTO(voteRepository, currentUserId, userRepository)
                .copy(hotScore = hotScores[post.id])
        }

        // Apply pagination manually (since we sorted in memory)
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, postDTOs.size)
        val paginatedPosts = if (start < postDTOs.size) {
            postDTOs.subList(start, end)
        } else {
            emptyList()
        }

        // Convert to PageDTO for caching
        return PageImpl(paginatedPosts, pageable, postDTOs.size.toLong()).toPageDTO()
    }

    fun findAll(pageable: Pageable): Page<PostResponseDTO> {
        val currentUserId = try {
            securityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null
        }

        return postRepository.findAll(pageable).map {
            it.toResponseDTO(voteRepository, currentUserId, userRepository)
        }
    }

    fun findByUserId(userId: Long, pageable: Pageable): Page<PostResponseDTO> {
        val currentUserId = try {
            securityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null
        }

        return postRepository.findByUserId(userId, pageable).map {
            it.toResponseDTO(voteRepository, currentUserId, userRepository)
        }
    }

    fun findById(postId: Long): PostResponseDTO {
        val currentUserId = try {
            securityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null
        }

        return postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found") }
            .toResponseDTO(voteRepository, currentUserId, userRepository)
    }

    fun save(postRequestDTO: PostRequestDTO, userId: Long): PostResponseDTO {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        val post = Post(
            title = postRequestDTO.title,
            content = postRequestDTO.content,
            mediaUrl = postRequestDTO.imageUrl,
            country = postRequestDTO.country,  // Country from request
            user = user
        )

        return postRepository.save(post)
            .toResponseDTO(voteRepository, userId, userRepository)
    }

    @CacheEvict(value = [CacheConfig.FEED_CACHE], allEntries = true)
    fun createPost(postRequestDTO: PostRequestDTO, file: MultipartFile?): PostResponseDTO {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("User not found") }

        var mediaUrl: String? = null
        if (file != null && !file.isEmpty) {
            val fileName = "${UUID.randomUUID()}-${file.originalFilename}"
            mediaUrl = s3Service.uploadFile(file, fileName)
        }

        // Country now comes from request, not user profile
        val post = Post(
            title = postRequestDTO.title,
            content = postRequestDTO.content,
            mediaUrl = mediaUrl,
            country = postRequestDTO.country,  // From request
            user = user
        )

        return postRepository.save(post)
            .toResponseDTO(voteRepository, currentUserId, userRepository)
    }

    @CacheEvict(value = [CacheConfig.FEED_CACHE], allEntries = true)
    fun update(
        postId: Long,
        updatedPostRequestDTO: PostRequestDTO,
        userId: Long,
        file: MultipartFile? = null,
        keepExistingImage: Boolean = true
    ): PostResponseDTO {
        val existingPost = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found") }

        // Authorization check: only post owner can update
        if (existingPost.user.id != userId) {
            throw SecurityException("You are not authorized to update this post")
        }

        // Handle image updates
        var newMediaUrl: String? = existingPost.mediaUrl

        if (file != null && !file.isEmpty) {
            // Delete old image if exists
            existingPost.mediaUrl?.let { oldUrl ->
                val oldFileName = oldUrl.substringAfterLast("/")
                s3Service.deleteFile(oldFileName)
            }

            // Upload new image
            val fileName = "${UUID.randomUUID()}-${file.originalFilename}"
            newMediaUrl = s3Service.uploadFile(file, fileName)
        } else if (!keepExistingImage) {
            // Remove existing image
            existingPost.mediaUrl?.let { oldUrl ->
                val oldFileName = oldUrl.substringAfterLast("/")
                s3Service.deleteFile(oldFileName)
            }
            newMediaUrl = null
        }

        val updatedPost = existingPost.copy(
            title = updatedPostRequestDTO.title,
            content = updatedPostRequestDTO.content,
            mediaUrl = newMediaUrl,
            country = updatedPostRequestDTO.country
        )

        return postRepository.save(updatedPost)
            .toResponseDTO(voteRepository, userId, userRepository)
    }

    @CacheEvict(value = [CacheConfig.FEED_CACHE], allEntries = true)
    fun delete(postId: Long, userId: Long) {
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found") }

        // Authorization check: only post owner can delete
        if (post.user.id != userId) {
            throw SecurityException("You are not authorized to delete this post")
        }

        post.mediaUrl?.let { url ->
            val fileName = url.substringAfterLast("/")
            s3Service.deleteFile(fileName)
        }

        postRepository.delete(post)
    }

    fun getPostsByCountry(country: Country): List<Post> {
        return postRepository.findByCountry(country)
    }

    fun getPostsByCountryWithPaging(
        country: Country,
        page: Int,
        size: Int
    ): Page<Post> {
        val pageable = PageRequest.of(page, size)
        return postRepository.findByCountry(country, pageable)
    }
}
