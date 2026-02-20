package com.example.wallet.common.auth

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authFilter: AuthFilter,
    private val authInterceptor: AuthInterceptor,
    private val authUserIdArgumentResolver: AuthUserIdArgumentResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authUserIdArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**")
    }

    @Bean
    fun authFilterRegistration(): FilterRegistrationBean<AuthFilter> =
        FilterRegistrationBean<AuthFilter>().apply {
            filter = authFilter
            order = Ordered.HIGHEST_PRECEDENCE
        }
}
