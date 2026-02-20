package com.example.wallet.common.exception

import com.example.wallet.common.error.ErrorCode

class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
