package com.ridelink.driver_vehicle_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverServiceAreaUpdateRequest {

    @NotBlank
    private String serviceArea;
}