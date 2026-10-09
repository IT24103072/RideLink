package com.ridelink.ride.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRideRequest {

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Pickup latitude is required")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    private Double pickupLongitude;

    @NotBlank(message = "Destination address is required")
    private String destinationAddress;

    @NotNull(message = "Destination latitude is required")
    private Double destinationLatitude;

    @NotNull(message = "Destination longitude is required")
    private Double destinationLongitude;

    @NotBlank(message = "Service area is required (e.g. Colombo, Kandy, Galle)")
    private String serviceArea;

    @NotBlank(message = "Vehicle type is required (SEDAN, SUV, VAN, MOTORCYCLE)")
    private String vehicleType;

    private Double estimatedDistanceKm;
}
