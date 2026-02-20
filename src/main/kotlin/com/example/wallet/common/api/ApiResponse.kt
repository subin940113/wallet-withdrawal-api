package com.example.wallet.common.api

import com.example.wallet.common.error.ErrorResponse

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null,
) {
    companion object {
        fun <T> ok(data: T) = ApiResponse(success = true, data = data, error = null)

        fun <T> error(error: ErrorResponse) = ApiResponse<T>(success = false, data = null, error = error)
    }
}
