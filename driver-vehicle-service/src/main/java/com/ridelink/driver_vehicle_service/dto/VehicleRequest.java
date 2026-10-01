package com.ridelink.driver_vehicle_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRequest {

    @NotBlank
    private String driverId;

    @NotBlank
    private String vehicleMake;

    @NotBlank
    private String vehicleModel;

    @NotBlank
    private String vehiclePlate;

    @Min(1)
    private int vehicleCapacity;

    @NotBlank
    private String vehicleType;
}