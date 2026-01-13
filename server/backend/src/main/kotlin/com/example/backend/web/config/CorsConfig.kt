package com.example.backend.web.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Allowed origins (add production URLs as needed)
        configuration.allowedOrigins = listOf(
            "http://localhost:3000",  // Next.js admin dev server
            "http://localhost:3001",  // Alternative port
            "http://127.0.0.1:3000"
        )

        // Allowed HTTP methods
        configuration.allowedMethods = listOf(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        )

        // Allowed headers
        configuration.allowedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin"
        )

        // Allow credentials (cookies, authorization headers)
        configuration.allowCredentials = true

        // Exposed headers (accessible from JS)
        configuration.exposedHeaders = listOf(
            "Authorization"
        )

        // Cache preflight response for 1 hour
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        return source
    }
}
