package com.example.wallet.presentation.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class WalletTransactionsResponse(
    @get:JsonProperty("transactions")
    val transactions: List<WalletTransactionsItemResponse>,
)
