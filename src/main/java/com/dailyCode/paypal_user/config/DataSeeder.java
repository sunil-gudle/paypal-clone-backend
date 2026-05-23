package com.dailyCode.paypal_user.config;

import com.dailyCode.paypal_user.user.entity.User;
import com.dailyCode.paypal_user.user.enums.UserRole;
import com.dailyCode.paypal_user.user.enums.UserStatus;
import com.dailyCode.paypal_user.user.repository.UserRepository;
import com.dailyCode.paypal_user.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    @Value("${app.admin.emails}")
    private String adminEmailsRaw;

    @Value("${app.admin.default-password}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> configuredAdmins = Arrays.stream(adminEmailsRaw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(e -> !e.isBlank())
                .toList();

        // 1. Create or promote everyone in the config list
        for (String email : configuredAdmins) {
            userRepository.findByEmail(email).ifPresentOrElse(
                    user -> {
                        if (user.getRole() != UserRole.ROLE_ADMIN) {
                            user.setRole(UserRole.ROLE_ADMIN);
                            userRepository.save(user);
                            log.info("Existing user promoted to ADMIN: {}", email);
                        } else {
                            log.info("Admin already exists, skipping: {}", email);
                        }
                    },
                    () -> {
                        // New admin — create user + wallet
                        User admin = User.builder()
                                .firstName("Admin")
                                .lastName("User")
                                .email(email)
                                .password(passwordEncoder.encode(defaultPassword))
                                .role(UserRole.ROLE_ADMIN)
                                .status(UserStatus.ACTIVE)
                                .emailVerified(true)
                                .build();
                        User saved = userRepository.save(admin);
                        walletService.createWallet(saved);
                        log.info("New admin created: {}", email);
                    }
            );
        }

        // 2. Demote any ROLE_ADMIN users NOT in the config list
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .filter(u -> !configuredAdmins.contains(u.getEmail().toLowerCase()))
                .forEach(u -> {
                    u.setRole(UserRole.ROLE_USER);
                    userRepository.save(u);
                    log.warn("Admin demoted (removed from config): {}", u.getEmail());
                });

        log.info("Admin sync complete. Active admins: {}", configuredAdmins);
    }
}
