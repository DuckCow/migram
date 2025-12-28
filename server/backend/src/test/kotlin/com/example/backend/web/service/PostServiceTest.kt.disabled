package com.example.backend.web.service

import com.example.backend.web.model.Country
import com.example.backend.web.model.Post
import com.example.backend.web.model.PostRequestDTO
import com.example.backend.web.model.User
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.service.PostService
import com.example.backend.web.service.S3Service
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PostServiceTest {

    @Mock
    private lateinit var postRepository: PostRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var securityUtils: SecurityUtils

    @Mock
    private lateinit var s3Service: S3Service

    @InjectMocks
    private lateinit var postService: PostService

    private lateinit var testUser: User
    private lateinit var testPost: Post
    private lateinit var testPostRequestDTO: PostRequestDTO

    @BeforeEach
    fun setUp() {
        testUser = User(
            id = 1L,
            name="sdf",
            email="sf",
            profilePicture = null,
            originCountry = Country.KOREA_SOUTH,
            currentCountry = Country.UNITED_STATES)

        testPost = Post(
            title = "Test Title",
            content = "Test Content",
            mediaUrl = "test-url",
            user = testUser
        )
        testPostRequestDTO = PostRequestDTO(
            title = "Test Title",
            content = "Test Content",
            imageUrl = "test-url"
        )
    }

    @Test
    fun `findAll should return page of posts`() {
        // Given
        val pageable = PageRequest.of(0, 10)
        val postPage = PageImpl(listOf(testPost))
        `when`(postRepository.findAll(pageable)).thenReturn(postPage)

        // When
        val result = postService.findAll(pageable)

        // Then
        assertNotNull(result)
        assertEquals(1, result.content.size)
        verify(postRepository).findAll(pageable)
    }

    @Test
    fun `findById should return post when exists`() {
        // Given
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(testPost))

        // When
        val result = postService.findById(1L)

        // Then
        assertNotNull(result)
        assertEquals(testPost.id, result.id)
        verify(postRepository).findById(1L)
    }

    @Test
    fun `findById should throw EntityNotFoundException when post doesn't exist`() {
        // Given
        `when`(postRepository.findById(1L)).thenReturn(Optional.empty())

        // When & Then
        val exception = assertThrows(EntityNotFoundException::class.java) {
            postService.findById(1L)
        }


    }

    @Test
    fun `createPost should save post with file`() {
        // Given
        val file = MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test".toByteArray()
        )
        val fileName = "test-filename"
        val mediaUrl = "test-url"

        `when`(securityUtils.getCurrentUserId()).thenReturn(1L)
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(testUser))
        `when`(s3Service.uploadFile(any(), any())).thenReturn(mediaUrl)
        `when`(postRepository.save(any())).thenReturn(testPost)

        // When
        val result = postService.createPost(testPostRequestDTO, file)


        // Then
        assertNotNull(result)
        verify(s3Service).uploadFile(eq(file), any())
        verify(postRepository).save(any())
    }

    @Test
    fun `delete should remove post and media`() {
        // Given
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(testPost))

        // When
        postService.delete(1L)

        // Then
        verify(s3Service).deleteFile(any())
        verify(postRepository).delete(testPost)
    }

    @Test
    fun `update should modify existing post`() {
        // Given
        val updatedPostRequestDTO = PostRequestDTO(
            title = "Updated Title",
            content = "Updated Content",
            imageUrl = "updated-url"
        )

        `when`(postRepository.findById(1L)).thenReturn(Optional.of(testPost))
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(testUser))
        `when`(postRepository.save(any())).thenReturn(testPost.copy(
            title = updatedPostRequestDTO.title,
            content = updatedPostRequestDTO.content,
            mediaUrl = updatedPostRequestDTO.imageUrl
        ))

        // When
        val result = postService.update(1L, updatedPostRequestDTO, 1L)

        // Then
        assertNotNull(result)
        assertEquals(updatedPostRequestDTO.title, result.title)
        assertEquals(updatedPostRequestDTO.content, result.content)
        verify(postRepository).save(any())
    }

    @Test
    fun `findByUserId should return page of user's posts`() {
        // Given
        val pageable = PageRequest.of(0, 10)
        val postPage = PageImpl(listOf(testPost))
        `when`(postRepository.findByUserId(1L, pageable)).thenReturn(postPage)

        // When
        val result = postService.findByUserId(1L, pageable)

        // Then
        assertNotNull(result)
        assertEquals(1, result.content.size)
        verify(postRepository).findByUserId(1L, pageable)
    }


    @Test
    fun `국가별 게시글 조회 테스트`() {
        // given
        val country = Country.UNITED_STATES
        whenever(postRepository.findByCountry(country))  // 메서드 이름 변경
            .thenReturn(listOf(testPost))

        // when
        val result = postService.getPostsByCountry(country)

        // then
        verify(postRepository).findByCountry(country)  // 메서드 이름 변경
        assertThat(result).hasSize(1)
    }

    // TODO: Fix after board refactoring - PostRequestDTO now requires boardId
    /*
    @Test
    fun `존재하지 않는 사용자의 게시글 생성 시 예외 발생 테스트`() {
        // given
        val postRequest = PostRequestDTO(
            title = "New Post",
            content = "New Content",
            imageUrl = null,
            boardId = 1L
        )
        whenever(securityUtils.getCurrentUserId()).thenReturn(999L)
        whenever(userRepository.findById(999L))
            .thenReturn(Optional.empty())

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            postService.createPost(postRequest, null)
        }
    }
    */

    // TODO: Fix after board refactoring - findByCountry method removed, replaced with board-based queries
    /*
    @Test
    fun `페이징된 국가별 게시글 조회 테스트`() {
        // given
        val country = Country.UNITED_STATES
        val pageable = PageRequest.of(0, 10)
        val posts = listOf(testPost)
        val page = PageImpl(posts)

        whenever(postRepository.findByCountry(country, pageable))
            .thenReturn(page)

        // when
        val result = postService.getPostsByCountryWithPaging(country, 0, 10)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].country).isEqualTo(country)
        verify(postRepository).findByCountry(country, pageable)
    }
    */
/*
    @Test
    fun `지역별 게시글 조회 테스트`() {
        // given
        val region = Country.Region.ASIA
        val asianCountries = Country.entries.filter { it.region == region }
        whenever(postRepository.findByCountryIn(asianCountries))
            .thenReturn(listOf(testPost))

        // when
        val result = postService.getPostsByRegion(region)

        // then
        assertThat(result).hasSize(1)
        verify(postRepository).findByCountryIn(asianCountries)
    }

 */

}