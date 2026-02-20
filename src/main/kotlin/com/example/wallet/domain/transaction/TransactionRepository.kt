package com.example.wallet.domain.transaction

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByTransactionId(transactionId: String): Transaction?

    fun findAllByWalletIdOrderByCreatedAtDesc(
        walletId: Long,
        pageable: Pageable,
    ): Page<Transaction>
}
