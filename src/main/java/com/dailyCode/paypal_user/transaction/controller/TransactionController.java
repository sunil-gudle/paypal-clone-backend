package com.dailyCode.paypal_user.transaction.controller;

import com.dailyCode.paypal_user.common.dto.ApiResponse;
import com.dailyCode.paypal_user.common.dto.PageResponse;
import com.dailyCode.paypal_user.transaction.dto.TransactionFilterRequest;
import com.dailyCode.paypal_user.transaction.dto.TransactionResponse;
import com.dailyCode.paypal_user.transaction.dto.TransferRequest;
import com.dailyCode.paypal_user.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Money transfer and transaction history")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @Operation(summary = "Send money to another user")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer successful", response));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a completed transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> refund(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        TransactionResponse response = transactionService.refund(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Refund processed", response));
    }

    @GetMapping
    @Operation(summary = "Get my transaction history with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute TransactionFilterRequest filter) {
        PageResponse<TransactionResponse> response = transactionService.getMyTransactions(userDetails.getUsername(), filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        TransactionResponse response = transactionService.getById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reference/{referenceId}")
    @Operation(summary = "Get transaction by reference ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByReference(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String referenceId) {
        TransactionResponse response = transactionService.getByReferenceId(referenceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
