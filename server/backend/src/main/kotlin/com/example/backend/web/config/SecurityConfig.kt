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

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Autowired
    lateinit var customOAuth2UserService: CustomOAuth2UserService

    @Bean
    @Profile("!test")
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/login", "/oauth2/**", "/login/**").permitAll()  // Allow access to login pages
                    .requestMatchers("/posts").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2Login{ oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }
                    .defaultSuccessUrl("/loginSuccess", true)
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



    //test용 config

    @Bean
    @Profile("test")
    fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .authorizeHttpRequests { authorize ->
                authorize
                    .anyRequest().permitAll()
            }
        return http.build()
    }



}



