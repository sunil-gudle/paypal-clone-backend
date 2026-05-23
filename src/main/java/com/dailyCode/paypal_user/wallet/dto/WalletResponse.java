package com.dailyCode.paypal_user.wallet.dto;

import com.dailyCode.paypal_user.wallet.enums.Currency;
import com.dailyCode.paypal_user.wallet.enums.WalletStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class WalletResponse {

    private Long id;
    private Long userId;
    private String ownerName;
    private BigDecimal balance;
    private Currency currency;
    private WalletStatus status;
    private LocalDateTime createdAt;
}
