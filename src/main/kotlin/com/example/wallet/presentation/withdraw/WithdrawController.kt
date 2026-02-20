package com.example.wallet.presentation.withdraw

import com.example.wallet.application.withdraw.WithdrawCommand
import com.example.wallet.application.withdraw.WithdrawUseCase
import com.example.wallet.common.api.ApiResponse
import com.example.wallet.common.auth.AuthUserId
import com.example.wallet.common.auth.Authenticated
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Authenticated
@RestController
@RequestMapping("/api/wallets")
class WithdrawController(
    private val withdrawUseCase: WithdrawUseCase,
) {
    @PostMapping("/{walletId}/withdraw")
    fun withdraw(
        @PathVariable walletId: Long,
        @AuthUserId userId: Long,
        @RequestBody @Valid request: WithdrawRequest,
    ): ResponseEntity<ApiResponse<WithdrawResponse>> {
        val balanceAfter =
            withdrawUseCase.withdraw(
                WithdrawCommand(
                    walletId = walletId,
                    ownerUserId = userId,
                    transactionId = request.transactionId,
                    amount = request.amount,
                ),
            )
        return ResponseEntity.ok(
            ApiResponse.ok(WithdrawResponse(balanceAfter = balanceAfter)),
        )
    }
}
