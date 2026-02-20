package com.example.wallet.domain.wallet

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class WalletRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
) : WalletRepositoryCustom {
    override fun decreaseIfEnoughReturningBalance(
        walletId: Long,
        amount: Long,
    ): Long? {
        val list =
            jdbcTemplate.query(
                """
                UPDATE wallets
                   SET balance = balance - ?,
                       updated_at = now()
                 WHERE id = ?
                   AND balance >= ?
                RETURNING balance
                """.trimIndent(),
                { rs, _ -> rs.getLong("balance") },
                amount,
                walletId,
                amount,
            )
        return list.singleOrNull()
    }
}
