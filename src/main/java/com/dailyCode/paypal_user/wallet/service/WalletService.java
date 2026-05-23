package com.dailyCode.paypal_user.wallet.service;

import com.dailyCode.paypal_user.common.exception.BadRequestException;
import com.dailyCode.paypal_user.common.exception.ResourceNotFoundException;
import com.dailyCode.paypal_user.user.entity.User;
import com.dailyCode.paypal_user.user.service.UserService;
import com.dailyCode.paypal_user.wallet.dto.TopUpRequest;
import com.dailyCode.paypal_user.wallet.dto.WalletResponse;
import com.dailyCode.paypal_user.wallet.entity.Wallet;
import com.dailyCode.paypal_user.wallet.enums.WalletStatus;
import com.dailyCode.paypal_user.wallet.mapper.WalletMapper;
import com.dailyCode.paypal_user.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserService userService;
    private final WalletMapper walletMapper;

    // Called internally after user registration
    @Transactional
    public Wallet createWallet(User user) {
        if (walletRepository.existsByUserId(user.getId())) {
            throw new BadRequestException("Wallet already exists for this user");
        }
        Wallet wallet = Wallet.builder().user(user).build();
        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet created for user: {}", user.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(String email) {
        User user = userService.findByEmail(email);
        Wallet wallet = findByUserId(user.getId());
        return walletMapper.toResponse(wallet);
    }

    @Transactional
    public WalletResponse topUp(String email, TopUpRequest request) {
        User user = userService.findByEmail(email);
        Wallet wallet = findByUserIdWithLock(user.getId());

        validateWalletActive(wallet);

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        log.info("Wallet topped up: {} + {}", email, request.getAmount());
        return walletMapper.toResponse(walletRepository.save(wallet));
    }

    // Internal — used by transaction service
    @Transactional
    public void debit(Long userId, BigDecimal amount) {
        Wallet wallet = findByUserIdWithLock(userId);
        validateWalletActive(wallet);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new com.dailyCode.paypal_user.common.exception.InsufficientFundsException(
                    "Insufficient balance. Available: " + wallet.getBalance());
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    @Transactional
    public void credit(Long userId, BigDecimal amount) {
        Wallet wallet = findByUserIdWithLock(userId);
        validateWalletActive(wallet);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    public Wallet findByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
    }

    private Wallet findByUserIdWithLock(Long userId) {
        return walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
    }

    private void validateWalletActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BadRequestException("Wallet is not active. Status: " + wallet.getStatus());
        }
    }
}
