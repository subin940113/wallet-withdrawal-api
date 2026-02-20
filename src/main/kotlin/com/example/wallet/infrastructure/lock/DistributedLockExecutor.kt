package com.example.wallet.infrastructure.lock

import com.example.wallet.common.error.BusinessException
import com.example.wallet.common.error.ErrorCode
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class DistributedLockExecutor(
    private val redissonClient: RedissonClient,
    @Value("\${wallet.lock.wait-seconds:1}")
    private val waitSeconds: Long,
    @Value("\${wallet.lock.lease-seconds:3}")
    private val leaseSeconds: Long,
) {
    fun <T> execute(
        key: String,
        block: () -> T,
    ): T {
        val lock = redissonClient.getLock(key)

        val acquired = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS)
        if (!acquired) {
            throw BusinessException(ErrorCode.WALLET_BUSY)
        }

        return try {
            block()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
