package com.dailyCode.paypal_user.wallet.entity;

import com.dailyCode.paypal_user.common.entity.BaseEntity;
import com.dailyCode.paypal_user.user.entity.User;
import com.dailyCode.paypal_user.wallet.enums.Currency;
import com.dailyCode.paypal_user.wallet.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Currency currency = Currency.USD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    // Optimistic locking — prevents race conditions on concurrent transfers
    @Version
    private Long version;
}
