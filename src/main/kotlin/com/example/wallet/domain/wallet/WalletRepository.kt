package com.example.wallet.domain.wallet

import org.springframework.data.jpa.repository.JpaRepository

interface WalletRepository : JpaRepository<Wallet, Long>, WalletRepositoryCustom {
    fun findByIdAndOwnerUserId(
        id: Long,
        ownerUserId: Long,
    ): Wallet?
}
