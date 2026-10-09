package com.ridelink.driver_vehicle_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "drivers")
public class Driver {

    @Id
    private String id;
    private String accountId;
    private String licenseNumber;
    private String vehicleMake;
    private String vehicleModel;
    private String vehiclePlate;
    private int vehicleCapacity;
    private String availabilityStatus;
    private String serviceArea;
    private Double currentLat;
    private Double currentLng;
    private String name;
    private String contactNumber;
    private String status;
}