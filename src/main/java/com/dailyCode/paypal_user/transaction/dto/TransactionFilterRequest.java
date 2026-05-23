package com.dailyCode.paypal_user.transaction.dto;

import com.dailyCode.paypal_user.transaction.enums.TransactionStatus;
import com.dailyCode.paypal_user.transaction.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionFilterRequest {

    private TransactionType type;
    private TransactionStatus status;
    private int page = 0;
    private int size = 10;
}
