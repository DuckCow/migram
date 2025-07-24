package com.example.backend.web.config

import com.example.backend.web.model.jwt.JwtAuthenticationFilter
import com.example.backend.web.model.jwt.JwtTokenProvider
import com.example.backend.web.service.CustomOAuth2UserService
import jakarta.servlet.http.HttpServletRequest
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.bind.annotation.GetMapping

@Configuration
@EnableWebSecurity
class SecurityConfig {


    @Autowired
    lateinit var customOAuth2UserService: CustomOAuth2UserService

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider // 추가

    @Autowired
    lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter




    // OAuth2 성공 핸들러 추가
    @Bean
    fun oauth2SuccessHandler(): AuthenticationSuccessHandler {
        return AuthenticationSuccessHandler { request, response, authentication ->
            println("=== OAuth2 authentication successful ===")

            try {
                val accessToken = jwtTokenProvider.generateAccessToken(authentication)
                val refreshToken = jwtTokenProvider.generateRefreshToken(authentication)

                println("Tokens generated successfully")

                val deepLinkUrl = "migram://oauth/callback?access_token=$accessToken&refresh_token=$refreshToken"
                println("Deep link URL: $deepLinkUrl")

                // HTML 페이지로 응답
                response.contentType = "text/html"
                response.writer.write("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>로그인 완료</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body>
                    <div style="text-align: center; margin-top: 50px; font-family: Arial, sans-serif;">
                        <h2>로그인이 완료되었습니다!</h2>
                        <p>앱으로 돌아가는 중...</p>
                        <p><a href="$deepLinkUrl">앱에서 열기</a></p>
                        <script>
                            // 자동으로 앱 열기 시도
                            setTimeout(function() {
                                window.location.href = '$deepLinkUrl';
                            }, 1000);
                            
                            // 3초 후에도 앱이 열리지 않으면 사용자에게 안내
                            setTimeout(function() {
                                document.body.innerHTML += '<p style="color: red;">앱이 자동으로 열리지 않으면 위의 "앱에서 열기" 링크를 클릭해주세요.</p>';
                            }, 3000);
                        </script>
                    </div>
                </body>
                </html>
            """.trimIndent())

            } catch (e: Exception) {
                println("Error in success handler: ${e.message}")
                e.printStackTrace()
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
                    .requestMatchers("/auth/**", "/posts").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }
                    .successHandler(oauth2SuccessHandler())
                    .failureUrl("/login?error=true")
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java) // 이 줄 추가!
            .logout { logout ->
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

