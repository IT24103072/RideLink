package com.ridelink.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                message = "Phone number must be 7-15 digits, optionally starting with +")
        String phoneNumber
) {
}
