package com.example.wallet.application.command

data class WithdrawWalletCommand(
    val walletId: Long,
    val ownerUserId: Long,
    val transactionId: String,
    val amount: Long,
)
