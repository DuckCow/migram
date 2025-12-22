package com.example.backend.web.model

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = "users")
data class User (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val name: String,
    val email: String,
    val profilePicture: String?,

    var nickname: String? = null,

    @Enumerated(EnumType.STRING)
    var originCountry: Country?,

    @Enumerated(EnumType.STRING)
    var currentCountry: Country?,

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    val roles: MutableSet<Role> = mutableSetOf(),


    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val posts: List<Post> = mutableListOf(),

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val comments: List<com.example.backend.web.model.Comment> = mutableListOf(),

    // Follow requests where this user is the target (received requests)
    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val followRequests: List<Follow> = mutableListOf(),

    // Follow requests where this user is the requester (sent requests)
    @OneToMany(mappedBy = "requester", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val sentFollowRequests: List<Follow> = mutableListOf()


) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> =
        roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }

    override fun getPassword(): String = "" // OAuth2 사용자는 비밀번호가 없음

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true



}

data class UserDTO(
    val id: Long,
    val name: String,
    val email: String,
    val profilePicture: String?,
    val nickname: String?,
    val originCountry: Country?,
    val currentCountry: Country?
) {
    fun toEntity(): User {
        return User(
            id = this.id,
            name = this.name,
            email = this.email,
            profilePicture = this.profilePicture,
            nickname = this.nickname,
            originCountry = this.originCountry,
            currentCountry = this.currentCountry
        )
    }
}

data class ProfileUpdateDTO(
    val nickname: String?,
    val originCountry: Country?,
    val currentCountry: Country?
)

data class UserProfileDTO(
    val user: UserDTO,
    val postCount: Long,
    val commentCount: Long
)
