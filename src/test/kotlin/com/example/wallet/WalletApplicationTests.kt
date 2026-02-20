package com.example.wallet

import com.example.wallet.support.AbstractPostgresContainerTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class WalletApplicationTests : AbstractPostgresContainerTest() {
    @Test
    fun contextLoads() {
    }
}
