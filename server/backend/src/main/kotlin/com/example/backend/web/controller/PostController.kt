package com.example.backend.web.controller

import com.example.backend.web.model.*
import com.example.backend.web.service.LikeService
import com.example.backend.web.service.PostService
import com.example.backend.web.service.UserService
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.security.Principal

@RestController
@RequestMapping("/posts")
class PostController(
    private val postService: PostService,
    private val likeService: LikeService
) {

    @GetMapping
    fun getAllPosts(pageable: Pageable): Page<PostResponseDTO> = postService.findAll(pageable)

    @GetMapping("/{postId}")
    fun getPostById(@PathVariable postId: Long): PostResponseDTO = postService.findById(postId)


    /*
    @PostMapping
    fun createPost(@RequestBody postRequestDTO: PostRequestDTO): PostResponseDTO {
        val userId = 1L // Replace with actual user ID retrieval logic
        return postService.save(postRequestDTO, userId)
    }

     */

    @PostMapping
    fun createPost(@RequestPart("post") postRequestDTO: PostRequestDTO,
                   @RequestPart("file", required = false) file: MultipartFile?): ResponseEntity<PostResponseDTO> {
        val createdPost = postService.createPost(postRequestDTO, file)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost)
    }

    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @RequestBody postRequestDTO: PostRequestDTO
    ): PostResponseDTO {
        val userId = 1L // Replace with actual user ID retrieval logic
        return postService.update(postId, postRequestDTO, userId)
    }

    @DeleteMapping("/{postId}")
    fun deletePost(@PathVariable postId: Long) {
        postService.delete(postId)
    }

    @PostMapping("/{postId}/like")
    fun togglePostLike(@PathVariable postId: Long): ResponseEntity<Boolean> {
        return try {
            val isLiked = likeService.togglePostLike(postId)
            ResponseEntity.ok(isLiked)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(false)
        } catch (e: EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{postId}/likes")
    fun getPostLikeUsers(
        @PathVariable postId: Long,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<UserDTO>> {
        return try {
            val likeUsers = likeService.getPostLikeUsers(postId, pageable)
            ResponseEntity.ok(likeUsers)
        } catch (e: EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{postId}/like-count")
    fun getPostLikeCount(@PathVariable postId: Long): ResponseEntity<Long> {
        return ResponseEntity.ok(likeService.getPostLikeCount(postId))
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
