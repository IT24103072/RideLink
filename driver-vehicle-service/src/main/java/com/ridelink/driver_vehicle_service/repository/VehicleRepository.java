package com.ridelink.driver_vehicle_service.repository;

import com.ridelink.driver_vehicle_service.model.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {

    boolean existsByVehiclePlateIgnoreCase(String vehiclePlate);

    boolean existsByVehiclePlateIgnoreCaseAndIdNot(String vehiclePlate, String id);

    List<Vehicle> findByDriverId(String driverId);
}