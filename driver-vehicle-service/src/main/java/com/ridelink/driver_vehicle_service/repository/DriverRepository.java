package com.ridelink.driver_vehicle_service.repository;

import com.ridelink.driver_vehicle_service.model.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DriverRepository extends MongoRepository<Driver, String> {

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, String id);

    List<Driver> findByAvailabilityStatus(String status);

    List<Driver> findByAvailabilityStatusAndServiceArea(String status, String serviceArea);
}