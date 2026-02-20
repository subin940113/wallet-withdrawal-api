package com.example.wallet.common.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val message: String,
) {
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "Invalid User-Id"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    UNAUTHORIZED(HttpStatus.FORBIDDEN, "Unauthorized"),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "Insufficient balance"),
    WALLET_BUSY(HttpStatus.CONFLICT, "Wallet is busy"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
}
