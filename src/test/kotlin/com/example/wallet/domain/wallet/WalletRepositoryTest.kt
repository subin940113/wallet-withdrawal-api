package com.example.wallet.domain.wallet

import com.example.wallet.support.AbstractPostgresContainerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
class WalletRepositoryTest : AbstractPostgresContainerTest() {
    @Autowired
    private lateinit var repository: WalletRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Test
    fun `잔액이 충분하면 차감되고 1을 반환한다`() {
        val wallet = repository.save(Wallet(ownerUserId = 1L, balance = 100L))
        entityManager.flush()
        entityManager.clear()

        val walletId = wallet.id!!
        val beforeUpdatedAt = repository.findById(walletId).orElseThrow().updatedAt

        val updated = repository.decreaseIfEnough(walletId, 60L)
        assertThat(updated).isEqualTo(1)

        entityManager.flush()
        entityManager.clear()

        val after = repository.findById(walletId).orElseThrow()

        assertThat(after.balance).isEqualTo(40L)
        assertThat(after.updatedAt).isAfterOrEqualTo(beforeUpdatedAt)
    }

    @Test
    fun `잔액이 부족하면 차감되지 않고 0을 반환한다`() {
        val wallet = repository.save(Wallet(ownerUserId = 1L, balance = 10L))
        entityManager.flush()
        entityManager.clear()

        val walletId = wallet.id!!

        val updated = repository.decreaseIfEnough(walletId, 60L)
        assertThat(updated).isEqualTo(0)

        entityManager.flush()
        entityManager.clear()

        val after = repository.findById(walletId).orElseThrow()

        assertThat(after.balance).isEqualTo(10L)
    }
}
