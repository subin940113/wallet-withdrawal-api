package com.example.wallet.application.withdraw

data class WithdrawCommand(
    val walletId: Long,
    val ownerUserId: Long,
    val transactionId: String,
    val amount: Long,
)
