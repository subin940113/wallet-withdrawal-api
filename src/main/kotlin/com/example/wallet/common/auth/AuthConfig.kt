package com.example.wallet.common.auth

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuthConfig {
    @Bean
    fun authFilter(): AuthFilter = AuthFilter()

    @Bean
    fun authInterceptor(objectMapper: ObjectMapper): AuthInterceptor = AuthInterceptor(objectMapper)

    @Bean
    fun authUserIdArgumentResolver(): AuthUserIdArgumentResolver = AuthUserIdArgumentResolver()
}
