package com.example.wallet.application

import com.example.wallet.domain.transaction.TransactionRepository
import com.example.wallet.domain.wallet.WalletRepository
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestWithdrawUseCaseConfig {
    @Bean
    fun testWithdrawWalletUseCase(
        walletRepository: WalletRepository,
        transactionRepository: TransactionRepository,
    ): TestWithdrawWalletUseCase = TestWithdrawWalletUseCase(walletRepository, transactionRepository)
}
