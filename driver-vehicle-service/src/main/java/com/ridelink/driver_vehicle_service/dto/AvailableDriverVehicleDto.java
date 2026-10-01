package com.ridelink.driver_vehicle_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableDriverVehicleDto {

    private String driverId;
    private String name;
    private String serviceArea;
    private Double latitude;
    private Double longitude;
    private String vehicleId;
    private String vehicleType;
    private String plateNumber;
    private int capacity;
}