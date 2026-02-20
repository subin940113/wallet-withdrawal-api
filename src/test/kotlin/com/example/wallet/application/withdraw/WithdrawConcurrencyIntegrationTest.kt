package com.example.wallet.application.withdraw

import com.example.wallet.TestcontainersConfiguration
import com.example.wallet.common.error.BusinessException
import com.example.wallet.common.error.ErrorCode
import com.example.wallet.domain.wallet.Wallet
import com.example.wallet.domain.wallet.WalletRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@ActiveProfiles("test")
class WithdrawConcurrencyIntegrationTest {
    @Autowired
    private lateinit var withdrawUseCase: WithdrawUseCase

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun `100개의 동시 출금 요청이 와도 잔액은 음수가 되지 않고 총 출금액은 초기 잔액을 초과하지 않는다`() {
        val ownerUserId = 1L
        val initialBalance = 1_000_000L
        val amount = 10_000L
        val threadCount = 100

        val walletId = createWallet(ownerUserId, initialBalance)

        val success = AtomicInteger(0)
        val failure = AtomicInteger(0)
        val totalWithdrawn = AtomicLong(0)

        runConcurrently(threadCount) {
            try {
                withdrawUseCase.withdraw(
                    WithdrawCommand(
                        walletId,
                        ownerUserId,
                        UUID.randomUUID().toString(),
                        amount,
                    ),
                )
                success.incrementAndGet()
                totalWithdrawn.addAndGet(amount)
            } catch (e: BusinessException) {
                failure.incrementAndGet()
                throw e
            }
        }

        val finalBalance = getBalance(walletId)

        assertThat(success.get()).isEqualTo(threadCount)
        assertThat(failure.get()).isEqualTo(0)
        assertThat(finalBalance).isEqualTo(0L)
        assertThat(totalWithdrawn.get()).isEqualTo(initialBalance)
        assertThat(finalBalance).isGreaterThanOrEqualTo(0L)
    }

    @Test
    fun `잔액보다 많은 금액을 동시에 출금 요청하면 일부는 반드시 잔액 부족 예외가 발생한다`() {
        val ownerUserId = 1L
        val initialBalance = 50_000L
        val amount = 10_000L
        val threadCount = 20

        val walletId = createWallet(ownerUserId, initialBalance)

        val insufficientCount = AtomicInteger(0)

        runConcurrently(threadCount) {
            try {
                withdrawUseCase.withdraw(
                    WithdrawCommand(
                        walletId,
                        ownerUserId,
                        UUID.randomUUID().toString(),
                        amount,
                    ),
                )
            } catch (e: BusinessException) {
                if (e.errorCode == ErrorCode.INSUFFICIENT_BALANCE) {
                    insufficientCount.incrementAndGet()
                } else {
                    throw e
                }
            }
        }

        val finalBalance = getBalance(walletId)

        assertThat(finalBalance).isGreaterThanOrEqualTo(0L)
        assertThat(insufficientCount.get()).isGreaterThan(0)
    }

    @Test
    fun `같은 transactionId로 동시에 요청하면 출금은 한 번만 수행되고 동일한 성공 결과가 반환된다`() {
        val ownerUserId = 1L
        val initialBalance = 1_000_000L
        val amount = 10_000L
        val sameTransactionId = UUID.randomUUID().toString()

        val walletId = createWallet(ownerUserId, initialBalance)

        val returnedBalances = ConcurrentLinkedQueue<Long>()

        runConcurrently(100) {
            val result =
                withdrawUseCase.withdraw(
                    WithdrawCommand(
                        walletId,
                        ownerUserId,
                        sameTransactionId,
                        amount,
                    ),
                )
            returnedBalances.add(result)
        }

        val finalBalance = getBalance(walletId)

        assertThat(finalBalance).isEqualTo(initialBalance - amount)
        assertThat(returnedBalances).hasSize(100)
        assertThat(returnedBalances.distinct()).containsExactly(initialBalance - amount)
    }

    @Test
    fun `잔액 부족 상태에서 같은 transactionId로 반복 요청하면 동일한 잔액 부족 예외가 재현된다`() {
        val ownerUserId = 1L
        val initialBalance = 5_000L
        val amount = 10_000L
        val sameTransactionId = UUID.randomUUID().toString()

        val walletId = createWallet(ownerUserId, initialBalance)

        assertThatThrownBy {
            withdrawUseCase.withdraw(
                WithdrawCommand(walletId, ownerUserId, sameTransactionId, amount),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE)

        assertThatThrownBy {
            withdrawUseCase.withdraw(
                WithdrawCommand(walletId, ownerUserId, sameTransactionId, amount),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE)

        assertThat(getBalance(walletId)).isEqualTo(initialBalance)
    }

    private fun createWallet(
        ownerUserId: Long,
        balance: Long,
    ): Long =
        transactionTemplate.execute {
            walletRepository.save(Wallet(ownerUserId, balance)).id!!
        }!!

    private fun getBalance(walletId: Long): Long =
        transactionTemplate.execute {
            walletRepository.findById(walletId).orElseThrow().balance
        }!!

    private fun runConcurrently(
        threadCount: Int,
        action: () -> Unit,
    ) {
        val pool = Executors.newFixedThreadPool(minOf(threadCount, 32))
        val startGate = CountDownLatch(1)
        val doneGate = CountDownLatch(threadCount)

        repeat(threadCount) {
            pool.submit {
                startGate.await()
                try {
                    action()
                } finally {
                    doneGate.countDown()
                }
            }
        }

        startGate.countDown()
        val finished = doneGate.await(30, TimeUnit.SECONDS)

        pool.shutdown()
        if (!finished) pool.shutdownNow()
        pool.awaitTermination(10, TimeUnit.SECONDS)

        assertThat(finished).isTrue()
    }
}
