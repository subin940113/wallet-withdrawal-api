package com.example.wallet.domain.wallet

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WalletRepository : JpaRepository<Wallet, Long> {
    /**
     * 잔액이 충분할 때만 차감한다.
     * @return 1(성공), 0(잔액 부족 또는 미존재)
     */
    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE Wallet w
           SET w.balance = w.balance - :amount,
               w.updatedAt = CURRENT_TIMESTAMP
         WHERE w.id = :walletId
           AND w.balance >= :amount
        """,
    )
    fun decreaseIfEnough(
        @Param("walletId") walletId: Long,
        @Param("amount") amount: Long,
    ): Int

    fun findByIdAndOwnerUserId(
        id: Long,
        ownerUserId: Long,
    ): Wallet?
}
