package com.ridelink.driver_vehicle_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverUpdateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String contactNumber;

    @NotBlank
    private String licenseNumber;

    @NotBlank
    private String serviceArea;
}