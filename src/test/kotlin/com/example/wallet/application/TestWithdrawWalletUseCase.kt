package com.example.wallet.application

import com.example.wallet.application.command.WithdrawWalletCommand
import com.example.wallet.common.error.ErrorCode
import com.example.wallet.common.exception.BusinessException
import com.example.wallet.domain.transaction.Transaction
import com.example.wallet.domain.transaction.TransactionRepository
import com.example.wallet.domain.transaction.TransactionStatus
import com.example.wallet.domain.wallet.WalletRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.locks.LockSupport

open class TestWithdrawWalletUseCase(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
) {
    @Transactional
    open fun withdraw(command: WithdrawWalletCommand) {
        validate(command)

        transactionRepository.findByTransactionId(command.transactionId)
            ?.let {
                restoreFromSnapshot(it)
                return
            }

        doWithdrawInternal(command)
    }

    private fun doWithdrawInternal(command: WithdrawWalletCommand) {
        transactionRepository.findByTransactionId(command.transactionId)
            ?.let {
                restoreFromSnapshot(it)
                return
            }

        val wallet =
            walletRepository.findByIdAndOwnerUserId(command.walletId, command.ownerUserId)
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        if (wallet.balance < command.amount) {
            persistSnapshotOrRestore(
                command,
                Transaction.failure(command, ErrorCode.INSUFFICIENT_BALANCE),
            )
            throw BusinessException(ErrorCode.INSUFFICIENT_BALANCE)
        }

        LockSupport.parkNanos(1_000_000)

        wallet.balance -= command.amount
        walletRepository.saveAndFlush(wallet)

        persistSnapshotOrRestore(
            command,
            Transaction.success(command, wallet.balance),
        )
    }

    private fun persistSnapshotOrRestore(
        command: WithdrawWalletCommand,
        snapshot: Transaction,
    ) {
        try {
            transactionRepository.save(snapshot)
        } catch (_: DataIntegrityViolationException) {
            val existing =
                transactionRepository.findByTransactionId(command.transactionId)
                    ?: throw BusinessException(ErrorCode.INTERNAL_ERROR)
            restoreFromSnapshot(existing)
        }
    }

    private fun restoreFromSnapshot(transaction: Transaction) {
        when (transaction.status) {
            TransactionStatus.SUCCESS -> Unit
            TransactionStatus.FAILED -> {
                val code =
                    transaction.failureReason
                        ?.let { runCatching { ErrorCode.valueOf(it) }.getOrNull() }
                        ?: ErrorCode.INTERNAL_ERROR
                throw BusinessException(code)
            }
        }
    }

    private fun validate(command: WithdrawWalletCommand) {
        if (command.amount <= 0L) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }
    }
}
