package com.example.wallet.application

import com.example.wallet.TestcontainersConfiguration
import com.example.wallet.application.command.WithdrawWalletCommand
import com.example.wallet.common.error.ErrorCode
import com.example.wallet.common.exception.BusinessException
import com.example.wallet.domain.wallet.Wallet
import com.example.wallet.domain.wallet.WalletRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class WithdrawWalletUseCaseIntegrationTest {
    @Autowired
    private lateinit var withdrawWalletUseCase: WithdrawWalletUseCase

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun `amount가 0이면 INVALID_REQUEST 예외를 던진다`() {
        val walletId = createWallet(1L, 10_000L)

        assertThatThrownBy {
            withdrawWalletUseCase.withdraw(
                WithdrawWalletCommand(walletId, 1L, "tx-1", 0L),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST)

        assertThat(getBalance(walletId)).isEqualTo(10_000L)
    }

    @Test
    fun `amount가 음수면 INVALID_REQUEST 예외를 던진다`() {
        val walletId = createWallet(1L, 10_000L)

        assertThatThrownBy {
            withdrawWalletUseCase.withdraw(
                WithdrawWalletCommand(walletId, 1L, "tx-1", -100L),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST)

        assertThat(getBalance(walletId)).isEqualTo(10_000L)
    }

    @Order(1)
    @Test
    fun `지갑 소유자가 아니면 UNAUTHORIZED 예외를 던진다`() {
        val ownerUserId = 1L
        val otherUserId = 999L
        val walletId = createWallet(ownerUserId, 10_000L)

        assertThatThrownBy {
            withdrawWalletUseCase.withdraw(
                WithdrawWalletCommand(walletId, otherUserId, "tx-1", 1000L),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED)

        assertThat(getBalance(walletId)).isEqualTo(10_000L)
    }

    @Test
    fun `소유자가 잔액 범위 내 출금하면 성공하고 잔액이 차감된다`() {
        val ownerUserId = 1L
        val initialBalance = 10_000L
        val amount = 3_000L
        val walletId = createWallet(ownerUserId, initialBalance)

        withdrawWalletUseCase.withdraw(
            WithdrawWalletCommand(walletId, ownerUserId, "tx-1", amount),
        )

        assertThat(getBalance(walletId)).isEqualTo(initialBalance - amount)
    }

    @Order(2)
    @Test
    fun `잔액 부족이면 INSUFFICIENT_BALANCE 예외를 던진다`() {
        val ownerUserId = 1L
        val initialBalance = 5_000L
        val amount = 10_000L
        val walletId = createWallet(ownerUserId, initialBalance)

        assertThatThrownBy {
            withdrawWalletUseCase.withdraw(
                WithdrawWalletCommand(walletId, ownerUserId, "tx-1", amount),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE)

        assertThat(getBalance(walletId)).isEqualTo(initialBalance)
    }

    @Test
    fun `같은 transactionId로 성공한 뒤 재요청하면 예외 없이 완료되고 잔액은 한 번만 차감된다`() {
        val ownerUserId = 1L
        val initialBalance = 10_000L
        val amount = 2_000L
        val sameTransactionId = UUID.randomUUID().toString()
        val walletId = createWallet(ownerUserId, initialBalance)

        withdrawWalletUseCase.withdraw(
            WithdrawWalletCommand(walletId, ownerUserId, sameTransactionId, amount),
        )
        withdrawWalletUseCase.withdraw(
            WithdrawWalletCommand(walletId, ownerUserId, sameTransactionId, amount),
        )

        assertThat(getBalance(walletId)).isEqualTo(initialBalance - amount)
    }

    @Test
    fun `같은 transactionId로 잔액 부족 실패한 뒤 재요청하면 동일한 INSUFFICIENT_BALANCE 예외가 재현된다`() {
        val ownerUserId = 1L
        val initialBalance = 5_000L
        val amount = 10_000L
        val sameTransactionId = UUID.randomUUID().toString()
        val walletId = createWallet(ownerUserId, initialBalance)

        assertThatThrownBy {
            withdrawWalletUseCase.withdraw(
                WithdrawWalletCommand(walletId, ownerUserId, sameTransactionId, amount),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE)

        assertThatThrownBy {
            withdrawWalletUseCase.withdraw(
                WithdrawWalletCommand(walletId, ownerUserId, sameTransactionId, amount),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE)

        assertThat(getBalance(walletId)).isEqualTo(initialBalance)
    }

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
                withdrawWalletUseCase.withdraw(
                    WithdrawWalletCommand(
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
                withdrawWalletUseCase.withdraw(
                    WithdrawWalletCommand(
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
    fun `같은 transactionId로 동시에 요청하면 출금은 한 번만 수행되고 잔액이 한 번만 차감된다`() {
        val ownerUserId = 1L
        val initialBalance = 1_000_000L
        val amount = 10_000L
        val sameTransactionId = UUID.randomUUID().toString()
        val walletId = createWallet(ownerUserId, initialBalance)

        runConcurrently(100) {
            withdrawWalletUseCase.withdraw(
                WithdrawWalletCommand(
                    walletId,
                    ownerUserId,
                    sameTransactionId,
                    amount,
                ),
            )
        }

        assertThat(getBalance(walletId)).isEqualTo(initialBalance - amount)
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
