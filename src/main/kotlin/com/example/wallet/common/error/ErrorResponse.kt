package com.example.wallet.common.error

import java.time.LocalDateTime

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun from(
            errorCode: ErrorCode,
            messageOverride: String? = null,
        ): ErrorResponse =
            ErrorResponse(
                code = errorCode.name,
                message = messageOverride ?: errorCode.message,
            )
    }
}
