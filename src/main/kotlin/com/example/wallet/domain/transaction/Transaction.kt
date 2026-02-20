package com.example.wallet.domain.transaction

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "transactions")
class Transaction(
    @Column(name = "transaction_id", nullable = false, length = 64)
    val transactionId: String,
    @Column(name = "wallet_id", nullable = false)
    val walletId: Long,
    @Column(nullable = false)
    val amount: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: TransactionStatus,
    @Column(name = "balance_after")
    val balanceAfter: Long? = null,
    @Column(name = "failure_reason", length = 64)
    val failureReason: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
