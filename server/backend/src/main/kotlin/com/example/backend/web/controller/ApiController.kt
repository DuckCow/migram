package com.example.backend.web.controller

import com.example.backend.web.model.Country
import com.example.backend.web.model.CustomOAuth2User
import com.example.backend.web.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api")
class ApiController(
    private val userService: UserService
){

    @PutMapping("/user/countries")
    fun updateUserCountries(
        @AuthenticationPrincipal user: CustomOAuth2User,
        @RequestBody request: CountryUpdateRequest
    ): ResponseEntity<Any> {
        userService.updateUserCountries(
            userId = user.getUser().id,
            originCountry = request.originCountry,
            currentCountry = request.currentCountry
        )
        return ResponseEntity.ok().build()
    }

    data class CountryUpdateRequest(
        val originCountry: Country,
        val currentCountry: Country
    )

}