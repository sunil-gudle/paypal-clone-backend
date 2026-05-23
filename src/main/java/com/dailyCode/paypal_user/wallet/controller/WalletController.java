package com.dailyCode.paypal_user.wallet.controller;

import com.dailyCode.paypal_user.common.dto.ApiResponse;
import com.dailyCode.paypal_user.wallet.dto.TopUpRequest;
import com.dailyCode.paypal_user.wallet.dto.WalletResponse;
import com.dailyCode.paypal_user.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet management")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Get my wallet details and balance")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal UserDetails userDetails) {
        WalletResponse response = walletService.getMyWallet(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/top-up")
    @Operation(summary = "Add funds to wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> topUp(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TopUpRequest request) {
        WalletResponse response = walletService.topUp(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Wallet topped up successfully", response));
    }
}
