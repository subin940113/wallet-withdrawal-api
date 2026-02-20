package com.example.wallet.presentation

import com.example.wallet.application.GetWalletTransactionsUseCase
import com.example.wallet.application.WithdrawWalletUseCase
import com.example.wallet.common.api.ApiResponse
import com.example.wallet.common.auth.AuthUserId
import com.example.wallet.common.auth.Authenticated
import com.example.wallet.presentation.dto.WalletTransactionsResponse
import com.example.wallet.presentation.dto.WalletWithdrawRequest
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Authenticated
@RestController
@RequestMapping("/api/wallets")
class WalletController(
    private val withdrawWalletUseCase: WithdrawWalletUseCase,
    private val getWalletTransactionsUseCase: GetWalletTransactionsUseCase,
) {
    @PostMapping("/{walletId}/withdraw")
    fun withdraw(
        @PathVariable walletId: Long,
        @AuthUserId userId: Long,
        @RequestBody @Valid request: WalletWithdrawRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        withdrawWalletUseCase.withdraw(request.toCommand(walletId, userId))
        return ResponseEntity.ok(ApiResponse.ok(Unit))
    }

    @GetMapping("/{walletId}/transactions")
    fun getTransactions(
        @PathVariable walletId: Long,
        @AuthUserId userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<WalletTransactionsResponse>> {
        val pageable = PageRequest.of(page, size)
        val result = getWalletTransactionsUseCase.getTransactions(walletId, userId, pageable)
        return ResponseEntity.ok(
            ApiResponse.ok(WalletTransactionsResponse(transactions = result.content)),
        )
    }
}
