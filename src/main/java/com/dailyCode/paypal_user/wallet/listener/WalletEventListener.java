package com.dailyCode.paypal_user.wallet.listener;

import com.dailyCode.paypal_user.user.event.UserRegisteredEvent;
import com.dailyCode.paypal_user.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventListener {

    private final WalletService walletService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Creating wallet for new user: {}", event.getUser().getEmail());
        walletService.createWallet(event.getUser());
    }
}
