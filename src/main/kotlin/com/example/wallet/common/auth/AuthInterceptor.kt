package com.example.wallet.common.auth

import com.example.wallet.common.api.ApiResponse
import com.example.wallet.common.error.ErrorCode
import com.example.wallet.common.error.ErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

class AuthInterceptor(
    private val objectMapper: ObjectMapper,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true
        if (!requiresAuth(handler)) return true

        when {
            request.getAttribute(AuthAttributes.AUTH_USER_ID_INVALID_KEY) == true -> {
                sendError(response, ErrorCode.INVALID_USER_ID)
                return false
            }
            request.getAttribute(AuthAttributes.AUTH_USER_ID_KEY) == null -> {
                sendError(response, ErrorCode.AUTH_REQUIRED)
                return false
            }
            else -> return true
        }
    }

    private fun requiresAuth(handler: HandlerMethod): Boolean {
        if (handler.beanType.getAnnotation(Authenticated::class.java) != null) return true
        if (handler.getMethodAnnotation(Authenticated::class.java) != null) return true
        return false
    }

    private fun sendError(
        response: HttpServletResponse,
        errorCode: ErrorCode,
    ) {
        response.status = errorCode.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        val body = ApiResponse.error<Nothing>(ErrorResponse.from(errorCode))
        objectMapper.writeValue(response.outputStream, body)
    }
}
