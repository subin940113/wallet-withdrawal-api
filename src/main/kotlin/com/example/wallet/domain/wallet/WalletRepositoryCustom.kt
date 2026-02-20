package com.example.wallet.domain.wallet

interface WalletRepositoryCustom {
    fun decreaseIfEnoughReturningBalance(
        walletId: Long,
        amount: Long,
    ): Long?
}
