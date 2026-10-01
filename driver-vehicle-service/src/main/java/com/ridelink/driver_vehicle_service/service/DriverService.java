package com.ridelink.driver_vehicle_service.service;

import com.ridelink.driver_vehicle_service.dto.DriverRequest;
import com.ridelink.driver_vehicle_service.dto.DriverUpdateRequest;
import com.ridelink.driver_vehicle_service.dto.AvailableDriverVehicleDto;
import com.ridelink.driver_vehicle_service.model.Driver;
import com.ridelink.driver_vehicle_service.model.Vehicle;
import com.ridelink.driver_vehicle_service.repository.DriverRepository;
import com.ridelink.driver_vehicle_service.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class DriverService {

    private static final String AVAILABLE = "AVAILABLE";
    private static final String OFFLINE = "OFFLINE";

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public DriverService(DriverRepository driverRepository, VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public Driver registerDriver(DriverRequest request) {
        if (request == null || request.getLicenseNumber() == null
                || request.getLicenseNumber().isBlank()) {
            throw new IllegalArgumentException("License number must not be blank");
        }

        String licenseNumber = request.getLicenseNumber().trim();
        if (driverRepository.existsByLicenseNumberIgnoreCase(licenseNumber)) {
            throw new IllegalStateException("A driver with this license number already exists");
        }

        Driver driver = new Driver(
                null,
                request.getAccountId(),
                licenseNumber,
                request.getVehicleMake(),
                request.getVehicleModel(),
                request.getVehiclePlate(),
                request.getVehicleCapacity(),
                OFFLINE,
                request.getServiceArea(),
                null,
                null,
                null,
                null,
                "ACTIVE"
        );
        return driverRepository.save(driver);
    }

    public Driver updateDriver(String id, DriverUpdateRequest request) {
        Driver driver = getDriver(id);
        String licenseNumber = request.getLicenseNumber().trim();
        if (!licenseNumber.equalsIgnoreCase(driver.getLicenseNumber())
                && driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot(licenseNumber, id)) {
            throw new IllegalStateException("A driver with this license number already exists");
        }

        driver.setName(request.getName().trim());
        driver.setContactNumber(request.getContactNumber().trim());
        driver.setLicenseNumber(licenseNumber);
        driver.setServiceArea(request.getServiceArea().trim());
        return driverRepository.save(driver);
    }

    public Driver updateDriverServiceArea(String id, String serviceArea) {
        if (serviceArea == null || serviceArea.isBlank()) {
            throw new IllegalArgumentException("Service area must not be blank");
        }

        Driver driver = getDriver(id);
        driver.setServiceArea(serviceArea.trim());
        return driverRepository.save(driver);
    }

    public Driver updateDriverStatus(String id, String status) {
        if (!Set.of("ACTIVE", "SUSPENDED", "INACTIVE").contains(status)) {
            throw new IllegalArgumentException(
                    "Status must be ACTIVE, SUSPENDED, or INACTIVE");
        }

        Driver driver = getDriver(id);
        driver.setStatus(status);
        return driverRepository.save(driver);
    }

    public Driver getDriver(String id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Driver not found: " + id));
    }

    public Driver updateAvailability(String id, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!AVAILABLE.equals(normalizedStatus) && !OFFLINE.equals(normalizedStatus)) {
            throw new IllegalArgumentException("Availability status must be AVAILABLE or OFFLINE");
        }

        Driver driver = getDriver(id);
        driver.setAvailabilityStatus(normalizedStatus);
        return driverRepository.save(driver);
    }

    public Driver updateLocation(String id, Double lat, Double lng) {
        if (lat == null || !Double.isFinite(lat) || lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (lng == null || !Double.isFinite(lng) || lng < -180 || lng > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        Driver driver = getDriver(id);
        driver.setCurrentLat(lat);
        driver.setCurrentLng(lng);
        return driverRepository.save(driver);
    }

    public List<AvailableDriverVehicleDto> getAvailableDrivers(
            String serviceArea,
            String vehicleType) {
        List<Driver> availableDrivers;
        if (serviceArea == null || serviceArea.isBlank()) {
            availableDrivers = driverRepository.findByAvailabilityStatus(AVAILABLE);
        } else {
            availableDrivers = driverRepository.findByAvailabilityStatusAndServiceArea(
                    AVAILABLE, serviceArea.trim());
        }

        String typeFilter = vehicleType == null || vehicleType.isBlank()
                ? null
                : vehicleType.trim();
        List<AvailableDriverVehicleDto> results = new ArrayList<>();
        for (Driver driver : availableDrivers) {
            for (Vehicle vehicle : vehicleRepository.findByDriverId(driver.getId())) {
                if (typeFilter != null && (vehicle.getVehicleType() == null
                        || !vehicle.getVehicleType().equalsIgnoreCase(typeFilter))) {
                    continue;
                }

                results.add(new AvailableDriverVehicleDto(
                        driver.getId(),
                        driver.getName(),
                        driver.getServiceArea(),
                        driver.getCurrentLat(),
                        driver.getCurrentLng(),
                        vehicle.getId(),
                        vehicle.getVehicleType(),
                        vehicle.getVehiclePlate(),
                        vehicle.getVehicleCapacity()
                ));
            }
        }
        return results;
    }
}