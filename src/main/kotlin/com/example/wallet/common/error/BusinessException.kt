package com.example.wallet.common.error

class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
