package com.example.backend.web.config

import com.example.backend.web.service.CustomOAuth2UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

@Configuration
@EnableWebSecurity
class SecurityConfig {


    @Autowired
    lateinit var customOAuth2UserService: CustomOAuth2UserService

    // OAuth2 성공 핸들러 추가
    @Bean
    fun oauth2SuccessHandler(): AuthenticationSuccessHandler {
        return AuthenticationSuccessHandler { request, response, authentication ->
            val userAgent = request.getHeader("User-Agent")
            if (userAgent != null && (userAgent.contains("Android") || userAgent.contains("iOS"))) {
                // 앱에서 요청한 경우 앱 스키마로 리디렉션
                response.sendRedirect("migram://oauth/callback")
            } else {
                response.sendRedirect("migram://oauth/callback")
                /*
                // 웹에서 요청한 경우 웹 페이지로 리디렉션
                response.sendRedirect("/loginSuccess")

                 */
            }
        }
    }

    @Bean
    @Profile("!test") // 프로덕션 환경용 SecurityFilterChain
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/login", "/oauth2/**", "/login/**").permitAll()
                    .requestMatchers("/posts").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2Login{ oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }
                    .successHandler(oauth2SuccessHandler()) // 여기에 핸들러 추가
                    .failureUrl("/login?error=true")
                    .authorizationEndpoint { authorization ->
                        authorization.baseUri("/oauth2/authorization")
                    }
            }
            .logout{ logout ->
                logout
                    .logoutSuccessUrl("/")
                    .permitAll()
            }

        return http.build()
    }

    @Bean
    @Profile("test") // 테스트 환경용 SecurityFilterChain
    fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().permitAll()
            }
        return http.build()
    }

    // 나머지 빈들은 모든 환경에서 사용 가능하도록 프로필 없이 유지
    @Bean
    fun userDetailsService(): UserDetailsService {
        val userDetailsService = InMemoryUserDetailsManager()
        val user = User.withUsername("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build()
        userDetailsService.createUser(user)
        return userDetailsService
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}

