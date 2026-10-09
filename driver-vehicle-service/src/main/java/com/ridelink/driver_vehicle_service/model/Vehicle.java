package com.ridelink.driver_vehicle_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vehicles")
public class Vehicle {

    @Id
    private String id;
    private String driverId;
    private String vehicleMake;
    private String vehicleModel;
    private String vehiclePlate;
    private int vehicleCapacity;
    private String vehicleType;
}