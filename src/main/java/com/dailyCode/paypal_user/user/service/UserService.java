package com.dailyCode.paypal_user.user.service;

import com.dailyCode.paypal_user.common.exception.BadRequestException;
import com.dailyCode.paypal_user.common.exception.ResourceNotFoundException;
import com.dailyCode.paypal_user.user.dto.RegisterRequest;
import com.dailyCode.paypal_user.user.dto.UpdateProfileRequest;
import com.dailyCode.paypal_user.user.dto.UserResponse;
import com.dailyCode.paypal_user.user.entity.User;
import com.dailyCode.paypal_user.user.enums.UserStatus;
import com.dailyCode.paypal_user.user.mapper.UserMapper;
import com.dailyCode.paypal_user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already in use");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());

        // Publish event so WalletService can create wallet without circular dependency
        eventPublisher.publishEvent(new com.dailyCode.paypal_user.user.event.UserRegisteredEvent(this, saved));

        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
        User user = findById(id);

        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already in use");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());

        return userMapper.toResponse(userRepository.save(user));
    }

    // Internal helper — used by other services
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    // ─── Admin operations ─────────────────────────────────────────────────────

    @Transactional
    public UserResponse updateStatus(Long userId, UserStatus status) {
        User user = findById(userId);
        user.setStatus(status);
        log.info("User {} status updated to: {}", user.getEmail(), status);
        return userMapper.toResponse(userRepository.save(user));
    }
}
