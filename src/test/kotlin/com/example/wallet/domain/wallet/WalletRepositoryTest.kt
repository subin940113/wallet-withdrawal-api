package com.example.wallet.domain.wallet

import com.example.wallet.TestcontainersConfiguration
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WalletRepositoryTest {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var repository: WalletRepository

    @Test
    fun `잔액이 충분하면 차감되고 차감 후 잔액을 반환한다`() {
        val wallet = repository.save(Wallet(ownerUserId = 1L, balance = 100L))
        entityManager.flush()
        entityManager.clear()

        val walletId = wallet.id!!
        val beforeUpdatedAt = repository.findById(walletId).orElseThrow().updatedAt

        val balanceAfter = repository.decreaseIfEnoughReturningBalance(walletId, 60L)
        assertThat(balanceAfter).isEqualTo(40L)

        entityManager.flush()
        entityManager.clear()

        val after = repository.findById(walletId).orElseThrow()
        assertThat(after.balance).isEqualTo(40L)
        assertThat(after.updatedAt).isAfterOrEqualTo(beforeUpdatedAt)
    }

    @Test
    fun `잔액이 부족하면 차감되지 않고 null을 반환한다`() {
        val wallet = repository.save(Wallet(ownerUserId = 1L, balance = 10L))
        entityManager.flush()
        entityManager.clear()

        val walletId = wallet.id!!

        val balanceAfter = repository.decreaseIfEnoughReturningBalance(walletId, 60L)
        assertThat(balanceAfter).isNull()

        entityManager.flush()
        entityManager.clear()

        val after = repository.findById(walletId).orElseThrow()
        assertThat(after.balance).isEqualTo(10L)
    }
}
