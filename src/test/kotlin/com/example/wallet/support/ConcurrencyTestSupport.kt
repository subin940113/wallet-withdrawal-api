package com.example.wallet.support

import com.example.wallet.domain.wallet.Wallet
import com.example.wallet.domain.wallet.WalletRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ConcurrencyTestSupport {
    fun runConcurrently(
        threadCount: Int,
        action: () -> Unit,
    ) {
        val pool = Executors.newFixedThreadPool(minOf(threadCount, 32))
        val readyGate = CountDownLatch(threadCount)
        val startGate = CountDownLatch(1)
        val doneGate = CountDownLatch(threadCount)

        repeat(threadCount) {
            pool.submit {
                readyGate.countDown()
                startGate.await()
                try {
                    action()
                } finally {
                    doneGate.countDown()
                }
            }
        }

        val ready = readyGate.await(10, TimeUnit.SECONDS)
        check(ready) { "threads did not become ready in time" }

        startGate.countDown()

        val ok = doneGate.await(30, TimeUnit.SECONDS)
        pool.shutdown()
        if (!ok) pool.shutdownNow()
        pool.awaitTermination(10, TimeUnit.SECONDS)
        check(ok) { "concurrent run did not finish in time" }
    }

    fun createWallet(
        transactionTemplate: TransactionTemplate,
        walletRepository: WalletRepository,
        ownerUserId: Long,
        balance: Long,
    ): Long =
        transactionTemplate.execute {
            walletRepository.save(Wallet(ownerUserId, balance)).id!!
        }!!

    fun getBalance(
        jdbcTemplate: JdbcTemplate,
        walletId: Long,
    ): Long = jdbcTemplate.queryForObject("SELECT balance FROM wallets WHERE id = ?", Long::class.java, walletId)!!
}
