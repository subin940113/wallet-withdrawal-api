package com.example.wallet.application

import com.example.wallet.application.command.WithdrawWalletCommand
import com.example.wallet.common.error.ErrorCode
import com.example.wallet.common.exception.BusinessException
import com.example.wallet.domain.transaction.Transaction
import com.example.wallet.domain.transaction.TransactionRepository
import com.example.wallet.domain.transaction.TransactionStatus
import com.example.wallet.domain.wallet.WalletRepository
import com.example.wallet.infrastructure.lock.DistributedLockExecutor
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class WithdrawWalletUseCase(
    private val transactionTemplate: TransactionTemplate,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val lockExecutor: DistributedLockExecutor,
) {
    fun withdraw(command: WithdrawWalletCommand) {
        validate(command)

        // 이미 처리된 거래라면 기존 결과 그대로 반환 (멱등: 성공이면 완료, 실패면 동일 예외)
        transactionRepository.findByTransactionId(command.transactionId)
            ?.let {
                restoreFromSnapshot(it)
                return
            }

        val lockKey = LOCK_KEY_PREFIX + command.walletId

        lockExecutor.execute(lockKey) {
            transactionTemplate.execute {
                doWithdrawInternal(command)
            } ?: throw BusinessException(ErrorCode.INTERNAL_ERROR)
        }
    }

    /**
     * 출금 비즈니스 로직을 수행한다.
     */
    private fun doWithdrawInternal(command: WithdrawWalletCommand) {
        // 락 획득 이후 다시 한 번 멱등 확인 (동시성 방어)
        transactionRepository.findByTransactionId(command.transactionId)
            ?.let {
                restoreFromSnapshot(it)
                return
            }

        // 지갑 소유자 검증
        authorize(command)

        // 잔액이 충분할 때만 차감하고, 차감 후 잔액을 반환
        val balanceAfter =
            walletRepository.decreaseIfEnoughReturningBalance(command.walletId, command.amount)

        // 잔액 부족이면 실패 스냅샷 저장 후 예외 발생
        if (balanceAfter == null) {
            persistSnapshotOrRestore(
                command,
                Transaction.failure(command, ErrorCode.INSUFFICIENT_BALANCE),
            )
            throw BusinessException(ErrorCode.INSUFFICIENT_BALANCE)
        }

        // 성공 스냅샷 저장
        persistSnapshotOrRestore(
            command,
            Transaction.success(command, balanceAfter),
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
     * 이미 처리된 거래 스냅샷: 성공이면 완료, 실패면 동일한 예외를 재현한다.
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

    /**
     * 요청자가 해당 지갑의 소유자인지 확인한다.
     */
    private fun authorize(command: WithdrawWalletCommand) {
        walletRepository.findByIdAndOwnerUserId(command.walletId, command.ownerUserId)
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
    }

    companion object {
        private const val LOCK_KEY_PREFIX = "WALLET_WITHDRAW:LOCK:WALLET:"
    }
}
