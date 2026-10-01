package com.ridelink.driver_vehicle_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatusUpdateRequest {

    @NotBlank
    @Pattern(regexp = "ACTIVE|SUSPENDED|INACTIVE")
    private String status;
}