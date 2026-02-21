package com.example.wallet.application

import com.example.wallet.application.command.WithdrawWalletCommand
import com.example.wallet.common.error.ErrorCode
import com.example.wallet.common.exception.BusinessException
import com.example.wallet.domain.transaction.Transaction
import com.example.wallet.domain.transaction.TransactionRepository
import com.example.wallet.domain.transaction.TransactionStatus
import com.example.wallet.domain.wallet.WalletRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 분산락·단일 SQL 조건 기반 차감 없이 처리하여
 * 동시성 제어 미적용 시 정합성 위반이 발생함을 부하테스트로 비교한다.
 */
@Service
class WithdrawWalletUseCase(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
) {
    @Transactional
    fun withdraw(command: WithdrawWalletCommand) {
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

        wallet.balance -= command.amount
        walletRepository.saveAndFlush(wallet)

        persistSnapshotOrRestore(
            command,
            Transaction.success(command, wallet.balance),
        )
    }

    /**
     * 결과 스냅샷을 저장한다.
     * transaction_id UNIQUE 제약 충돌이 발생하면,
     * 이미 처리된 기존 스냅샷을 조회하여 동일한 결과로 복구한다.
     */
    private fun persistSnapshotOrRestore(
        command: WithdrawWalletCommand,
        snapshot: Transaction,
    ) {
        try {
            transactionRepository.save(snapshot)
        } catch (_: DataIntegrityViolationException) {
            val existingTransaction =
                transactionRepository.findByTransactionId(command.transactionId)
                    ?: throw BusinessException(ErrorCode.INTERNAL_ERROR)

            restoreFromSnapshot(existingTransaction)
        }
    }

    /**
     * 이미 처리된 거래 스냅샷 : 성공이면 완료, 실패면 동일한 예외를 재현한다.
     */
    private fun restoreFromSnapshot(transaction: Transaction) {
        when (transaction.status) {
            TransactionStatus.SUCCESS -> Unit
            TransactionStatus.FAILED -> {
                val errorCode =
                    transaction.failureReason
                        ?.let { runCatching { ErrorCode.valueOf(it) }.getOrNull() }
                        ?: ErrorCode.INTERNAL_ERROR
                throw BusinessException(errorCode)
            }
        }
    }

    /**
     * 출금 요청의 기본 유효성을 검증한다.
     */
    private fun validate(command: WithdrawWalletCommand) {
        if (command.amount <= 0L) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }
    }
}
