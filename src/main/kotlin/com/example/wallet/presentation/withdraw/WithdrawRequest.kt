package com.example.wallet.presentation.withdraw

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class WithdrawRequest(
    @field:NotBlank(message = "transactionId is required")
    @field:Size(min = 1, max = 64)
    val transactionId: String,
    @field:Min(1, message = "amount must be positive")
    val amount: Long,
)
