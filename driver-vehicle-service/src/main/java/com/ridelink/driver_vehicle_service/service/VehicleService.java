package com.ridelink.driver_vehicle_service.service;

import com.ridelink.driver_vehicle_service.dto.VehicleRequest;
import com.ridelink.driver_vehicle_service.model.Vehicle;
import com.ridelink.driver_vehicle_service.repository.VehicleRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final Validator validator;

    public VehicleService(VehicleRepository vehicleRepository, Validator validator) {
        this.vehicleRepository = vehicleRepository;
        this.validator = validator;
    }

    public Vehicle registerVehicle(VehicleRequest request) {
        validateRequest(request);

        String vehiclePlate = request.getVehiclePlate().trim();
        if (vehicleRepository.existsByVehiclePlateIgnoreCase(vehiclePlate)) {
            throw new IllegalStateException("A vehicle with this plate already exists");
        }

        Vehicle vehicle = new Vehicle(
                null,
                request.getDriverId(),
                request.getVehicleMake(),
                request.getVehicleModel(),
                vehiclePlate,
                request.getVehicleCapacity(),
                request.getVehicleType().trim()
        );
        return vehicleRepository.save(vehicle);
    }

    public Vehicle getVehicle(String id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vehicle not found: " + id));
    }

    public List<Vehicle> getVehiclesByDriver(String driverId) {
        return vehicleRepository.findByDriverId(driverId);
    }

    public Vehicle updateVehicle(String id, VehicleRequest request) {
        Vehicle vehicle = getVehicle(id);
        validateRequest(request);

        String vehiclePlate = request.getVehiclePlate().trim();
        String currentPlate = vehicle.getVehiclePlate();
        boolean plateChanged = currentPlate == null
                || !currentPlate.trim().equalsIgnoreCase(vehiclePlate);
        if (plateChanged && vehicleRepository
                .existsByVehiclePlateIgnoreCaseAndIdNot(vehiclePlate, id)) {
            throw new IllegalStateException("A vehicle with this plate already exists");
        }

        vehicle.setDriverId(request.getDriverId());
        vehicle.setVehicleMake(request.getVehicleMake());
        vehicle.setVehicleModel(request.getVehicleModel());
        vehicle.setVehiclePlate(vehiclePlate);
        vehicle.setVehicleCapacity(request.getVehicleCapacity());
        vehicle.setVehicleType(request.getVehicleType().trim());
        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(String id) {
        Vehicle vehicle = getVehicle(id);
        vehicleRepository.delete(vehicle);
    }

    private void validateRequest(VehicleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Vehicle request must not be null");
        }

        Set<ConstraintViolation<VehicleRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String messages = violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid vehicle request: " + messages);
        }
    }
}