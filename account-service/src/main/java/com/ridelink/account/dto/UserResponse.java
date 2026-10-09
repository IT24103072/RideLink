package com.ridelink.account.dto;

import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.model.UserAccount;

import java.time.Instant;

/** Public view of an account. Never includes the password hash. */
public record UserResponse(
        String id,
        String email,
        String fullName,
        String phoneNumber,
        Role role,
        AccountStatus status,
        Instant createdAt
) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
