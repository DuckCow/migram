package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
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
    private val securityUtils: SecurityUtils,
    private val s3Service: S3Service
) {

    fun findAll(pageable: Pageable): Page<PostResponseDTO> = postRepository.findAll(pageable).map { it.toResponseDTO() }

    fun findByUserId(userId: Long, pageable: Pageable): Page<PostResponseDTO> =
        postRepository.findByUserId(userId, pageable).map { it.toResponseDTO() }

    fun findById(postId: Long): PostResponseDTO =
        postRepository.findById(postId).orElseThrow { EntityNotFoundException("Post not found") }.toResponseDTO()

    fun save(postRequestDTO: PostRequestDTO, userId: Long): PostResponseDTO {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val post = Post(
            user = user,
            title = postRequestDTO.title,
            content = postRequestDTO.content,
            mediaUrl = postRequestDTO.imageUrl
        )
        return postRepository.save(post).toResponseDTO()
    }

    fun createPost(postRequestDTO: PostRequestDTO, file: MultipartFile?): PostResponseDTO {
        val currentUserId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId).orElseThrow { EntityNotFoundException("User not found") }

        var mediaUrl: String? = null
        if (file != null && !file.isEmpty) {
            val fileName = "${UUID.randomUUID()}-${file.originalFilename}"
            mediaUrl = s3Service.uploadFile(file, fileName)
        }

        val post = Post(
            title = postRequestDTO.title,
            content = postRequestDTO.content,
            mediaUrl = mediaUrl,
            user = user
        )

        return postRepository.save(post).toResponseDTO()
    }


    fun update(postId: Long, updatedPostRequestDTO: PostRequestDTO, userId: Long): PostResponseDTO {
        val existingPost = postRepository.findById(postId).orElseThrow { EntityNotFoundException("Post not found") }
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val updatedPost = existingPost.copy(
            title = updatedPostRequestDTO.title,
            content = updatedPostRequestDTO.content,
            mediaUrl = updatedPostRequestDTO.imageUrl
        )
        return postRepository.save(updatedPost).toResponseDTO()
    }


    fun delete(postId: Long) {
        val post = postRepository.findById(postId).orElseThrow { EntityNotFoundException("Post not found") }

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