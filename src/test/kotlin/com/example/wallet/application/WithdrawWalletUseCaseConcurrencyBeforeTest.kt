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
import java.util.concurrent.atomic.AtomicLong

@Import(TestcontainersConfiguration::class, TestWithdrawUseCaseConfig::class)
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WithdrawWalletUseCaseConcurrencyBeforeTest {
    @Autowired
    private lateinit var testWithdrawWalletUseCase: TestWithdrawWalletUseCase

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
            testWithdrawWalletUseCase.withdraw(WithdrawWalletCommand(walletId, 1L, "tx-1", 0L))
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
            testWithdrawWalletUseCase.withdraw(WithdrawWalletCommand(walletId, 999L, "tx-1", 1000L))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED)
        assertThat(ConcurrencyTestSupport.getBalance(jdbcTemplate, walletId)).isEqualTo(10_000L)
    }

    @Test
    fun `잔액이 부족하면 INSUFFICIENT_BALANCE 예외를 던지고 잔액은 변하지 않는다`() {
        val ownerUserId = 1L
        val initialBalance = 5_000L
        val amount = 10_000L
        val walletId = ConcurrencyTestSupport.createWallet(transactionTemplate, walletRepository, ownerUserId, initialBalance)
        assertThatThrownBy {
            testWithdrawWalletUseCase.withdraw(WithdrawWalletCommand(walletId, ownerUserId, "tx-1", amount))
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
            testWithdrawWalletUseCase.withdraw(WithdrawWalletCommand(walletId, ownerUserId, sameTxId, amount))
        }
        val finalBalance = ConcurrencyTestSupport.getBalance(jdbcTemplate, walletId)
        assertThat(finalBalance).isEqualTo(initialBalance - amount)
    }

    @Test
    fun `서로 다른 transactionId로 동시에 출금 요청하면 정합성 위반이 발생할 수 있다`() {
        val ownerUserId = 1L
        val initialBalance = 50_000L
        val amount = 10_000L
        val threadCount = 32
        val trialCount = 50
        var violationObserved = false
        var sampleFinalBalance = 0L
        var negativeBalanceObserved = false
        var overWithdrawalObserved = false
        var balanceMismatchObserved = false
        repeat(trialCount) {
            val walletId = ConcurrencyTestSupport.createWallet(transactionTemplate, walletRepository, ownerUserId, initialBalance)
            val totalWithdrawn = AtomicLong(0)
            ConcurrencyTestSupport.runConcurrently(threadCount) {
                try {
                    testWithdrawWalletUseCase.withdraw(
                        WithdrawWalletCommand(walletId, ownerUserId, UUID.randomUUID().toString(), amount),
                    )
                    totalWithdrawn.addAndGet(amount)
                } catch (_: BusinessException) {
                }
            }
            val finalBalance = ConcurrencyTestSupport.getBalance(jdbcTemplate, walletId)
            val total = totalWithdrawn.get()
            sampleFinalBalance = finalBalance
            if (finalBalance < 0L) negativeBalanceObserved = true
            if (total > initialBalance) overWithdrawalObserved = true
            if (finalBalance != initialBalance - total) balanceMismatchObserved = true
            if (finalBalance < 0L || total > initialBalance || finalBalance != initialBalance - total) {
                violationObserved = true
            }
        }
        assertThat(violationObserved).describedAs("정합성 위반(잔액 음수/총 출금 초과/잔액 불일치)이 trialCount 내에서 관찰되어야 함").isTrue()
        val violationTypes =
            buildList {
                if (negativeBalanceObserved) add("잔액음수")
                if (overWithdrawalObserved) add("총출금초과")
                if (balanceMismatchObserved) add("잔액불일치")
            }.joinToString(", ")
        println("Before: trialCount=$trialCount, violationObserved=$violationObserved, sampleFinalBalance=$sampleFinalBalance")
        println("Before 위반 유형: $violationTypes")
    }
}
