package com.example.wallet.application

import com.example.wallet.TestcontainersConfiguration
import com.example.wallet.application.command.WithdrawWalletCommand
import com.example.wallet.common.error.ErrorCode
import com.example.wallet.common.exception.BusinessException
import com.example.wallet.domain.wallet.Wallet
import com.example.wallet.domain.wallet.WalletRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GetWalletTransactionsUseCaseIntegrationTest {
    @Autowired
    private lateinit var getWalletTransactionsUseCase: GetWalletTransactionsUseCase

    @Autowired
    private lateinit var withdrawWalletUseCase: WithdrawWalletUseCase

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Test
    fun `지갑 소유자가 아니면 UNAUTHORIZED 예외를 던진다`() {
        val wallet = walletRepository.save(Wallet(ownerUserId = 1L, balance = 1000L))
        val walletId = wallet.id!!

        assertThatThrownBy {
            getWalletTransactionsUseCase.getTransactions(walletId, 999L, PageRequest.of(0, 20))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED)
    }

    @Test
    fun `지갑 소유자이면 해당 지갑 거래 목록을 최신순으로 반환한다`() {
        val ownerUserId = 1L
        val wallet = walletRepository.save(Wallet(ownerUserId = ownerUserId, balance = 10_000L))
        val walletId = wallet.id!!

        withdrawWalletUseCase.withdraw(
            WithdrawWalletCommand(walletId, ownerUserId, "tx-1", 1000L),
        )
        withdrawWalletUseCase.withdraw(
            WithdrawWalletCommand(walletId, ownerUserId, "tx-2", 2000L),
        )

        val result =
            getWalletTransactionsUseCase.getTransactions(
                walletId,
                ownerUserId,
                PageRequest.of(0, 20),
            )

        assertThat(result.content).hasSize(2)
        assertThat(result.content[0].transactionId).isEqualTo("tx-2")
        assertThat(result.content[0].withdrawalAmount).isEqualTo(2000L)
        assertThat(result.content[1].transactionId).isEqualTo("tx-1")
        assertThat(result.content[1].withdrawalAmount).isEqualTo(1000L)
    }
}
