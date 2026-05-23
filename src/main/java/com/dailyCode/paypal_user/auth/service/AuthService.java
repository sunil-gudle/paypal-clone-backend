package com.dailyCode.paypal_user.auth.service;

import com.dailyCode.paypal_user.auth.dto.AuthResponse;
import com.dailyCode.paypal_user.auth.dto.LoginRequest;
import com.dailyCode.paypal_user.auth.dto.RefreshTokenRequest;
import com.dailyCode.paypal_user.auth.util.JwtUtil;
import com.dailyCode.paypal_user.common.exception.UnauthorizedException;
import com.dailyCode.paypal_user.user.dto.RegisterRequest;
import com.dailyCode.paypal_user.user.dto.UserResponse;
import com.dailyCode.paypal_user.user.entity.User;
import com.dailyCode.paypal_user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;

    public UserResponse register(RegisterRequest request) {
        return userService.register(request);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userService.findByEmail(request.getEmail());

        Map<String, Object> claims = Map.of("role", user.getRole().name());
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), claims);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Store refresh token in Redis with TTL
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getEmail(),
                refreshToken,
                7,
                TimeUnit.DAYS
        );

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + email);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new UnauthorizedException("Refresh token not recognized");
        }

        User user = userService.findByEmail(email);
        Map<String, Object> claims = Map.of("role", user.getRole().name());
        String newAccessToken = jwtUtil.generateAccessToken(email, claims);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        // Rotate refresh token
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + email,
                newRefreshToken,
                7,
                TimeUnit.DAYS
        );

        return buildAuthResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String email) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);
        log.info("User logged out: {}", email);
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiryMs() / 1000)
                .build();
    }
}
