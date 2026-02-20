package com.example.wallet.common.exception

import com.example.wallet.common.api.ApiResponse
import com.example.wallet.common.error.ErrorCode
import com.example.wallet.common.error.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        log.info("BusinessException: errorCode={}", ex.errorCode, ex)
        val code = ex.errorCode
        return ResponseEntity
            .status(code.httpStatus)
            .body(ApiResponse.error(ErrorResponse.from(code)))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        log.info("MethodArgumentNotValidException: fieldErrors={}", ex.bindingResult.fieldErrors, ex)
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: ErrorCode.INVALID_REQUEST.message
        val error = ErrorResponse.from(ErrorCode.INVALID_REQUEST, message)
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.httpStatus).body(ApiResponse.error(error))
    }

    @ExceptionHandler(Throwable::class)
    fun handleUnexpected(ex: Throwable): ResponseEntity<ApiResponse<Nothing>> {
        log.info("Unexpected exception", ex)
        val error = ErrorResponse.from(ErrorCode.INTERNAL_ERROR)
        return ResponseEntity
            .status(ErrorCode.INTERNAL_ERROR.httpStatus)
            .body(ApiResponse.error(error))
    }
}
