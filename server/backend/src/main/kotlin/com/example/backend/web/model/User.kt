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

    @Enumerated(EnumType.STRING)
    val originCountry: Country?,

    @Enumerated(EnumType.STRING)
    val currentCountry: Country?,

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    val roles: MutableSet<Role> = mutableSetOf(),


    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val posts: List<Post> = mutableListOf(),

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val comments: List<com.example.backend.web.model.Comment> = mutableListOf(),

    @OneToMany(mappedBy = "follower", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val followers: List<Follow> = mutableListOf(),

    @OneToMany(mappedBy = "following", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val followings: List<Follow> = mutableListOf()


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
    val originCountry: Country?,
    val currentCountry: Country?
) {
    fun toEntity(): User {
        return User(
            id = this.id,
            name = this.name,
            email = this.email,
            profilePicture = this.profilePicture,
            originCountry = this.originCountry,
            currentCountry = this.currentCountry
        )
    }
}
