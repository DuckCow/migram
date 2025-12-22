package com.example.backend.web.controller

import com.example.backend.web.model.Country
import com.example.backend.web.model.CustomOAuth2User
import com.example.backend.web.service.UserService
import com.example.backend.web.util.SecurityUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api")
class ApiController(
    private val userService: UserService,
    private val securityUtils: SecurityUtils
){

    @GetMapping("/countries")
    fun getCountries(): List<CountryResponse> {
        return Country.values().map { country ->
            CountryResponse(
                code = country.code,
                displayName = country.displayName,
                dialCode = country.dialCode,
                region = country.region.name
            )
        }
    }

    @PutMapping("/user/countries")
    fun updateUserCountries(
        @RequestBody request: CountryUpdateRequest?
    ): ResponseEntity<Any> {
        println("=== updateUserCountries called ===")
        println("Request: $request")
        println("Current Country: ${request?.currentCountry}")

        if (request == null || request.currentCountry == null) {
            println("Request is null or currentCountry is null")
            return ResponseEntity.badRequest().build()
        }

        try {
            val userId = securityUtils.getCurrentUserId()
            println("User ID from SecurityUtils: $userId")

            userService.updateUserCountries(
                userId = userId,
                originCountry = null,
                currentCountry = request.currentCountry
            )
            return ResponseEntity.ok().build()
        } catch (e: Exception) {
            println("Error updating countries: ${e.message}")
            e.printStackTrace()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    data class CountryUpdateRequest(
        val currentCountry: Country?
    )

}


data class CountryResponse(
    val code: String,
    val displayName: String,
    val dialCode: String,
    val region: String
)