package com.example.backend.web.service

import com.example.backend.web.model.CustomOAuth2User
import com.example.backend.web.model.Role
import com.example.backend.web.model.User
import com.example.backend.web.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service


@Service
class CustomOAuth2UserService(private val userRepository : UserRepository) : DefaultOAuth2UserService(
) {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauth2User = super.loadUser(userRequest)

        val email = oauth2User.attributes["email"] as String

        val user = userRepository.findByEmail(email) ?: run {
            val name = oauth2User.attributes["name"] as String
            val picture = oauth2User.attributes["picture"] as String?

            val newUser = User(
                name = name,
                email = email,
                profilePicture = picture,
                originCountry = null,
                currentCountry = null,
                roles = mutableSetOf(Role.USER)
            )
            userRepository.save(newUser)
        }

        return com.example.backend.web.model.CustomOAuth2User(user)
    }

}