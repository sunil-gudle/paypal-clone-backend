package com.dailyCode.paypal_user.transaction.service;

import com.dailyCode.paypal_user.common.dto.PageResponse;
import com.dailyCode.paypal_user.common.exception.BadRequestException;
import com.dailyCode.paypal_user.common.exception.ResourceNotFoundException;
import com.dailyCode.paypal_user.transaction.dto.TransactionFilterRequest;
import com.dailyCode.paypal_user.transaction.dto.TransactionResponse;
import com.dailyCode.paypal_user.transaction.dto.TransferRequest;
import com.dailyCode.paypal_user.transaction.entity.Transaction;
import com.dailyCode.paypal_user.transaction.enums.TransactionStatus;
import com.dailyCode.paypal_user.transaction.enums.TransactionType;
import com.dailyCode.paypal_user.transaction.mapper.TransactionMapper;
import com.dailyCode.paypal_user.transaction.repository.TransactionRepository;
import com.dailyCode.paypal_user.user.entity.User;
import com.dailyCode.paypal_user.user.repository.UserRepository;
import com.dailyCode.paypal_user.user.service.UserService;
import com.dailyCode.paypal_user.wallet.entity.Wallet;
import com.dailyCode.paypal_user.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse transfer(String senderEmail, TransferRequest request) {
        // Prevent self-transfer
        if (senderEmail.equalsIgnoreCase(request.getRecipientEmail())) {
            throw new BadRequestException("Cannot transfer money to yourself");
        }

        // Idempotency — if same key used twice, return existing transaction
        String referenceId = StringUtils.hasText(request.getIdempotencyKey())
                ? request.getIdempotencyKey()
                : UUID.randomUUID().toString();

        if (transactionRepository.existsByReferenceId(referenceId)) {
            Transaction existing = transactionRepository.findByReferenceId(referenceId).get();
            log.warn("Duplicate transaction request, returning existing: {}", referenceId);
            return transactionMapper.toResponse(existing);
        }

        User sender   = userService.findByEmail(senderEmail);

        // Explicit check for receiver — gives a clearer error than generic "User not found"
        if (!userRepository.existsByEmail(request.getRecipientEmail())) {
            throw new ResourceNotFoundException(
                    "Recipient not found: " + request.getRecipientEmail() + ". Please check the email and try again.");
        }
        User receiver = userService.findByEmail(request.getRecipientEmail());
        Wallet senderWallet = walletService.findByUserId(sender.getId());

        // Build transaction record first as PENDING
        Transaction txn = Transaction.builder()
                .referenceId(referenceId)
                .sender(sender)
                .receiver(receiver)
                .amount(request.getAmount())
                .currency(senderWallet.getCurrency())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .note(request.getNote())
                .build();

        txn = transactionRepository.save(txn);

        try {
            walletService.debit(sender.getId(), request.getAmount());
            walletService.credit(receiver.getId(), request.getAmount());

            txn.setStatus(TransactionStatus.COMPLETED);
            log.info("Transfer completed: {} -> {} | amount: {} | ref: {}",
                    senderEmail, request.getRecipientEmail(), request.getAmount(), referenceId);
        } catch (Exception ex) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setFailureReason(ex.getMessage());
            transactionRepository.save(txn);
            log.error("Transfer failed: {}", ex.getMessage());
            throw ex;
        }

        return transactionMapper.toResponse(transactionRepository.save(txn));
    }

    @Transactional
    public TransactionResponse refund(String requesterEmail, Long transactionId) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        // Only the sender of the original transaction can request a refund
        if (!original.getSender().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new BadRequestException("You are not authorized to refund this transaction");
        }

        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new BadRequestException("Only completed transactions can be refunded");
        }

        if (original.getType() == TransactionType.REFUND) {
            throw new BadRequestException("Cannot refund a refund transaction");
        }

        // Reverse the money flow
        walletService.debit(original.getReceiver().getId(), original.getAmount());
        walletService.credit(original.getSender().getId(), original.getAmount());

        // Mark original as refunded
        original.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.save(original);

        // Create refund transaction record
        Transaction refundTxn = Transaction.builder()
                .referenceId(UUID.randomUUID().toString())
                .sender(original.getReceiver())
                .receiver(original.getSender())
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .type(TransactionType.REFUND)
                .status(TransactionStatus.COMPLETED)
                .note("Refund for transaction: " + original.getReferenceId())
                .originalTransactionId(original.getId())
                .build();

        log.info("Refund processed for transaction: {}", transactionId);
        return transactionMapper.toResponse(transactionRepository.save(refundTxn));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReferenceId(String referenceId) {
        Transaction txn = transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + referenceId));
        return transactionMapper.toResponse(txn);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getMyTransactions(String email, TransactionFilterRequest filter) {
        User user = userService.findByEmail(email);
        PageRequest pageable = PageRequest.of(filter.getPage(), filter.getSize());

        Page<Transaction> page;

        if (filter.getType() != null) {
            page = transactionRepository.findAllByUserIdAndType(user.getId(), filter.getType(), pageable);
        } else if (filter.getStatus() != null) {
            page = transactionRepository.findAllByUserIdAndStatus(user.getId(), filter.getStatus(), pageable);
        } else {
            page = transactionRepository.findAllByUserId(user.getId(), pageable);
        }

        return PageResponse.from(page.map(transactionMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id, String requesterEmail) {
        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));

        // Users can only see their own transactions
        boolean isSender   = txn.getSender()   != null && txn.getSender().getEmail().equalsIgnoreCase(requesterEmail);
        boolean isReceiver = txn.getReceiver() != null && txn.getReceiver().getEmail().equalsIgnoreCase(requesterEmail);

        if (!isSender && !isReceiver) {
            throw new BadRequestException("Access denied to this transaction");
        }

        return transactionMapper.toResponse(txn);
    }
}
