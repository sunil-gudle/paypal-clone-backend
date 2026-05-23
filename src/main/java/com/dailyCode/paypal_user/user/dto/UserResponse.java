package com.dailyCode.paypal_user.user.dto;

import com.dailyCode.paypal_user.user.enums.UserRole;
import com.dailyCode.paypal_user.user.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
