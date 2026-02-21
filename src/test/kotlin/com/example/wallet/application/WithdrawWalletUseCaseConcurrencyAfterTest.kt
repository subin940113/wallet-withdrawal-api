package com.example.wallet.application

import com.example.wallet.TestcontainersConfiguration
import com.example.wallet.application.command.WithdrawWalletCommand
import com.example.wallet.common.error.ErrorCode
import com.example.wallet.common.exception.BusinessException
import com.example.wallet.domain.wallet.WalletRepository
import com.example.wallet.support.ConcurrencyTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WithdrawWalletUseCaseConcurrencyAfterTest {
    @Autowired
    private lateinit var withdrawWalletUseCase: WithdrawWalletUseCase

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `amount가 0이면 INVALID_REQUEST 예외를 던진다`() {
        val walletId = ConcurrencyTestSupport.createWallet(transactionTemplate, walletRepository, 1L, 10_000L)
        assertThatThrownBy {
            withdrawWalletUseCase.withdraw(WithdrawWalletCommand(walletId, 1L, "tx-1", 0L))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST)
        assertThat(ConcurrencyTestSupport.getBalance(jdbcTemplate, walletId)).isEqualTo(10_000L)
    }

    @Test
    fun `지갑 소유자가 아니면 UNAUTHORIZED 예외를 던진다`() {
        val walletId = ConcurrencyTestSupport.createWallet(transactionTemplate, walletRepository, 1L, 10_000L)
        assertThatThrownBy {
            withdrawWalletUseCase.withdraw(WithdrawWalletCommand(walletId, 999L, "tx-1", 1000L))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED)
        assertThat(ConcurrencyTestSupport.getBalance(jdbcTemplate, walletId)).isEqualTo(10_000L)
    }

    @Test
    fun `잔액이 충분하면 출금에 성공하고 잔액이 차감된다`() {
        val ownerUserId = 1L
        val initialBalance = 10_000L
        val amount = 3_000L
        val walletId = ConcurrencyTestSupport.createWallet(transactionTemplate, walletRepository, ownerUserId, initialBalance)
        withdrawWalletUseCase.withdraw(WithdrawWalletCommand(walletId, ownerUserId, "tx-1", amount))
        assertThat(ConcurrencyTestSupport.getBalance(jdbcTemplate, walletId)).isEqualTo(initialBalance - amount)
    }

    @Test
    fun `잔액이 부족하면 INSUFFICIENT_BALANCE 예외를 던지고 잔액은 변하지 않는다`() {
        val ownerUserId = 1L
        val initialBalance = 5_000L
        val amount = 10_000L
        val walletId = ConcurrencyTestSupport.createWallet(transactionTemplate, walletRepository, ownerUserId, initialBalance)
        assertThatThrownBy {
            withdrawWalletUseCase.withdraw(WithdrawWalletCommand(walletId, ownerUserId, "tx-1", amount))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE)
        assertThat(ConcurrencyTestSupport.getBalance(jdbcTemplate, walletId)).isEqualTo(initialBalance)
    }

    @Test
    fun `같은 transactionId로 동시에 출금 요청해도 출금은 1번만 반영된다`() {
        val ownerUserId = 1L
        val initialBalance = 10_000L
        val amount = 2_000L
        val sameTxId = UUID.randomUUID().toString()
        val walletId = ConcurrencyTestSupport.createWallet(transactionTemplate, walletRepository, ownerUserId, initialBalance)
        val threadCount = 20
        ConcurrencyTestSupport.runConcurrently(threadCount) {
            withdrawWalletUseCase.withdraw(WithdrawWalletCommand(walletId, ownerUserId, sameTxId, amount))
        }
        val finalBalance = ConcurrencyTestSupport.getBalance(jdbcTemplate, walletId)
        assertThat(finalBalance).isEqualTo(initialBalance - amount)
        println("After(같은 txId): threadCount=$threadCount, finalBalance=$finalBalance")
    }

    @Test
    fun `서로 다른 transactionId로 동시에 출금 요청해도 잔액은 음수가 되지 않는다`() {
        val ownerUserId = 1L
        val initialBalance = 50_000L
        val amount = 10_000L
        val threadCount = 20
        val walletId = ConcurrencyTestSupport.createWallet(transactionTemplate, walletRepository, ownerUserId, initialBalance)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val totalWithdrawn = AtomicLong(0)
        ConcurrencyTestSupport.runConcurrently(threadCount) {
            try {
                withdrawWalletUseCase.withdraw(
                    WithdrawWalletCommand(walletId, ownerUserId, UUID.randomUUID().toString(), amount),
                )
                successCount.incrementAndGet()
                totalWithdrawn.addAndGet(amount)
            } catch (_: BusinessException) {
                failureCount.incrementAndGet()
            }
        }
        val finalBalance = ConcurrencyTestSupport.getBalance(jdbcTemplate, walletId)
        assertThat(finalBalance).isGreaterThanOrEqualTo(0L)
        assertThat(totalWithdrawn.get()).isLessThanOrEqualTo(initialBalance)
        assertThat(finalBalance).isEqualTo(initialBalance - totalWithdrawn.get())
        println(
            "After(다른 txId): threadCount=$threadCount, successCount=${successCount.get()}, " +
                "failureCount=${failureCount.get()}, finalBalance=$finalBalance",
        )
    }
}
