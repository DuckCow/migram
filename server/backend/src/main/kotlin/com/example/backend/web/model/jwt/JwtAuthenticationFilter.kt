package com.example.backend.web.model.jwt

import com.example.backend.web.service.UserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userService: UserService
) : OncePerRequestFilter() {

    private companion object {
        private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            logger.debug("=== JWT Filter processing: ${request.method} ${request.requestURI} ===")
            val token = getTokenFromRequest(request)

            if (token != null) {
                logger.debug("Token found, validating...")
                if (jwtTokenProvider.validateToken(token)) {
                    logger.debug("Token is valid, extracting email...")
                    val email = jwtTokenProvider.getEmailFromToken(token)
                    logger.debug("Email extracted: $email")

                    val user = userService.findEntityByEmail(email)
                    logger.debug("User found: ${user != null}")

                    if (user != null) {
                        // enum Role을 SimpleGrantedAuthority로 변환
                        val authorities = user.roles.map { role ->
                            SimpleGrantedAuthority("ROLE_${role.name}")
                        }

                        val authentication = UsernamePasswordAuthenticationToken(
                            user.email,
                            null,
                            authorities
                        )
                        SecurityContextHolder.getContext().authentication = authentication
                        logger.debug("Authentication set successfully for user: ${user.email}")
                    } else {
                        logger.warn("User not found for email: $email")
                    }
                } else {
                    logger.warn("Token validation failed")
                }
            } else {
                logger.debug("No token found in request")
            }
        } catch (e: Exception) {
            logger.error("Cannot set user authentication: ${e.message}", e)
        }

        filterChain.doFilter(request, response)
    }

    private fun getTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}