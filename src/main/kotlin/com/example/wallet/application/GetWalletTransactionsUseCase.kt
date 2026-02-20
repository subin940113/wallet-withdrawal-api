package com.example.wallet.application

import com.example.wallet.common.error.ErrorCode
import com.example.wallet.common.exception.BusinessException
import com.example.wallet.domain.transaction.TransactionRepository
import com.example.wallet.domain.wallet.WalletRepository
import com.example.wallet.presentation.dto.WalletTransactionsItemResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GetWalletTransactionsUseCase(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
) {
    fun getTransactions(
        walletId: Long,
        ownerUserId: Long,
        pageable: Pageable,
    ): Page<WalletTransactionsItemResponse> {
        walletRepository.findByIdAndOwnerUserId(walletId, ownerUserId)
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        return transactionRepository
            .findAllByWalletIdOrderByCreatedAtDesc(walletId, pageable)
            .map(WalletTransactionsItemResponse.Companion::from)
    }
}
