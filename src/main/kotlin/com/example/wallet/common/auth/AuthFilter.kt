package com.example.wallet.common.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class AuthFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userIdHeader = request.getHeader(USER_ID_HEADER)
        if (userIdHeader.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }
        val value = userIdHeader.trim().toLongOrNull()
        when {
            value == null || value <= 0L -> {
                request.setAttribute(AuthAttributes.AUTH_USER_ID_INVALID_KEY, true)
            }
            else -> {
                request.setAttribute(AuthAttributes.AUTH_USER_ID_KEY, value)
            }
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        const val USER_ID_HEADER = "User-Id"
    }
}
