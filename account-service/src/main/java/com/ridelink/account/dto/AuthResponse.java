package com.ridelink.account.dto;

import com.ridelink.account.model.Role;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String userId,
        Role role
) {
}
