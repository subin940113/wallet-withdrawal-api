package com.example.wallet.common.auth

import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class AuthUserIdArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(AuthUserId::class.java) &&
            parameter.parameterType == Long::class.javaObjectType

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long =
        (webRequest.getAttribute(AuthAttributes.AUTH_USER_ID_KEY, 0) as? Long)
            ?: throw IllegalStateException("AUTH_USER_ID not set")
}

object AuthAttributes {
    const val AUTH_USER_ID_KEY = "AUTH_USER_ID"
    const val AUTH_USER_ID_INVALID_KEY = "AUTH_USER_ID_INVALID"
}
