package com.dailyCode.paypal_user.transaction.entity;

import com.dailyCode.paypal_user.common.entity.BaseEntity;
import com.dailyCode.paypal_user.transaction.enums.TransactionStatus;
import com.dailyCode.paypal_user.transaction.enums.TransactionType;
import com.dailyCode.paypal_user.user.entity.User;
import com.dailyCode.paypal_user.wallet.enums.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_sender", columnList = "sender_id"),
        @Index(name = "idx_txn_receiver", columnList = "receiver_id"),
        @Index(name = "idx_txn_reference", columnList = "reference_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String referenceId;  // UUID — idempotency key

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    private String note;

    private String failureReason;

    // Points to original transaction if this is a refund
    @Column(name = "original_transaction_id")
    private Long originalTransactionId;
}
