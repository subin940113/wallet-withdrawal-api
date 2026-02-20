package com.example.wallet.presentation.dto

import com.example.wallet.application.command.WithdrawWalletCommand
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class WalletWithdrawRequest(
    @param:JsonProperty("transaction_id")
    @field:NotBlank(message = "transactionId is required")
    @field:Size(min = 1, max = 64)
    val transactionId: String,
    @param:JsonProperty("amount")
    @field:Min(1, message = "amount must be positive")
    val amount: Long,
) {
    fun toCommand(
        walletId: Long,
        ownerUserId: Long,
    ): WithdrawWalletCommand =
        WithdrawWalletCommand(
            walletId = walletId,
            ownerUserId = ownerUserId,
            transactionId = transactionId,
            amount = amount,
        )
}
