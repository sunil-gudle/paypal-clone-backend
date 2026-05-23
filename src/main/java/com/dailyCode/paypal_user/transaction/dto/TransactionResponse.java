package com.dailyCode.paypal_user.transaction.dto;

import com.dailyCode.paypal_user.transaction.enums.TransactionStatus;
import com.dailyCode.paypal_user.transaction.enums.TransactionType;
import com.dailyCode.paypal_user.wallet.enums.Currency;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionResponse {

    private Long id;
    private String referenceId;
    private String senderEmail;
    private String senderName;
    private String receiverEmail;
    private String receiverName;
    private BigDecimal amount;
    private Currency currency;
    private TransactionType type;
    private TransactionStatus status;
    private String note;
    private String failureReason;
    private Long originalTransactionId;
    private LocalDateTime createdAt;
}
