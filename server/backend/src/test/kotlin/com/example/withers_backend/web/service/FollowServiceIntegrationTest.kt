package com.example.withers_backend.web.service

import com.example.backend.web.model.User
import com.example.backend.web.repository.FollowRepository
import com.example.backend.web.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional


/*
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FollowServiceIntegrationTest {

    @Autowired
    private lateinit var followService: FollowService

    @Autowired
    private lateinit var followRepository: FollowRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var follower: User
    private lateinit var following: User

    @BeforeEach
    fun setup() {
        follower = userRepository.save(User(name = "Follower", email = "follower@example.com", profilePicture = null))
        following = userRepository.save(User(name = "Following", email = "following@example.com",profilePicture = null))

        // SecurityContext 설정
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val authentication = UsernamePasswordAuthenticationToken(follower.id.toString(), null, authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `follow should create a new follow relationship`() {
        // When
        val result = followService.follow(following.id)

        // Then
        assertThat(result).isTrue()
        val follow = followRepository.findByFollowerAndFollowing(follower, following)
        assertThat(follow).isNotNull
        assertThat(follow?.follower?.id).isEqualTo(follower.id)
        assertThat(follow?.following?.id).isEqualTo(following.id)
    }

    @Test
    fun `unfollow should remove existing follow relationship`() {
        // Given
        followService.follow(following.id)

        // When
        val result = followService.unfollow(following.id)

        // Then
        assertThat(result).isTrue()
        val follow = followRepository.findByFollowerAndFollowing(follower, following)
        assertThat(follow).isNull()
    }

    @Test
    fun `getFollowers should return correct followers`() {
        // Given
        val anotherFollower = userRepository.save(User(name = "Another Follower", email = "another@example.com", profilePicture = null))
        followService.follow(following.id)

        // 다른 사용자로 팔로우
        val anotherAuthentication = UsernamePasswordAuthenticationToken(anotherFollower.id.toString(), null, listOf(SimpleGrantedAuthority("ROLE_USER")))
        SecurityContextHolder.getContext().authentication = anotherAuthentication
        followService.follow(following.id)

        // 원래 사용자로 복귀
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(follower.id.toString(), null, listOf(SimpleGrantedAuthority("ROLE_USER")))

        // When
        val followers = followService.getFollowers(following.id, Pageable.unpaged())

        // Then
        assertThat(followers.content).hasSize(2)
        assertThat(followers.content.map { it.id }).containsExactlyInAnyOrder(follower.id, anotherFollower.id)
    }

    @Test
    fun `getFollowings should return correct followings`() {
        // Given
        val anotherFollowing = userRepository.save(User(name = "Another Following", email = "another2@example.com",profilePicture = null))
        followService.follow(following.id)
        followService.follow(anotherFollowing.id)


        // When
        val followings = followService.getFollowings(follower.id, Pageable.unpaged())

        // Then
        assertThat(followings.content).hasSize(2)
        assertThat(followings.content.map { it.id }).containsExactlyInAnyOrder(following.id, anotherFollowing.id)


    }

}

*/
