package com.example.wallet.presentation.dto

import com.example.wallet.domain.transaction.Transaction
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class WalletTransactionsItemResponse(
    @get:JsonProperty("transaction_id")
    val transactionId: String,
    @get:JsonProperty("wallet_id")
    val walletId: Long,
    @get:JsonProperty("withdrawal_amount")
    val withdrawalAmount: Long,
    @get:JsonProperty("balance")
    val balance: Long?,
    @get:JsonProperty("withdrawal_date")
    val withdrawalDate: LocalDateTime,
) {
    companion object {
        fun from(transaction: Transaction): WalletTransactionsItemResponse =
            WalletTransactionsItemResponse(
                transactionId = transaction.transactionId,
                walletId = transaction.walletId,
                withdrawalAmount = transaction.amount,
                balance = transaction.balanceAfter,
                withdrawalDate = transaction.createdAt,
            )
    }
}
