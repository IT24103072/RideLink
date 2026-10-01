package com.ridelink.driver_vehicle_service.service;

import com.ridelink.driver_vehicle_service.dto.VehicleRequest;
import com.ridelink.driver_vehicle_service.model.Vehicle;
import com.ridelink.driver_vehicle_service.repository.VehicleRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private Validator validator;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void registerVehicle_savesVehicle() {
        VehicleRequest request = validRequest();
        allowValidRequest(request);
        when(vehicleRepository.existsByVehiclePlateIgnoreCase("ABC-123")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle result = vehicleService.registerVehicle(request);

        assertEquals("driver-1", result.getDriverId());
        assertEquals("ABC-123", result.getVehiclePlate());
        assertEquals(4, result.getVehicleCapacity());
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void registerVehicle_rejectsDuplicatePlate() {
        VehicleRequest request = validRequest();
        allowValidRequest(request);
        when(vehicleRepository.existsByVehiclePlateIgnoreCase("ABC-123")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> vehicleService.registerVehicle(request));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void registerVehicle_rejectsInvalidRequest() {
        VehicleRequest request = new VehicleRequest(
            "", "Toyota", "Camry", "ABC-123", 0, "Sedan");
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VehicleService validatingService = new VehicleService(
                    vehicleRepository, factory.getValidator());

            assertThrows(IllegalArgumentException.class,
                    () -> validatingService.registerVehicle(request));
        }
        verify(vehicleRepository, never()).existsByVehiclePlateIgnoreCase(any(String.class));
    }

    @Test
    void getVehicle_returnsVehicleWhenFound() {
        Vehicle vehicle = vehicle("vehicle-1", "ABC-123");
        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(vehicle));

        Vehicle result = vehicleService.getVehicle("vehicle-1");

        assertEquals(vehicle, result);
    }

    @Test
    void getVehicle_throwsWhenVehicleDoesNotExist() {
        when(vehicleRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> vehicleService.getVehicle("missing"));
    }

    @Test
    void getVehiclesByDriver_returnsVehiclesForDriver() {
        List<Vehicle> vehicles = List.of(vehicle("vehicle-1", "ABC-123"));
        when(vehicleRepository.findByDriverId("driver-1")).thenReturn(vehicles);

        List<Vehicle> result = vehicleService.getVehiclesByDriver("driver-1");

        assertEquals(vehicles, result);
        verify(vehicleRepository).findByDriverId("driver-1");
    }

    @Test
    void updateVehicle_updatesAndSavesVehicle() {
        Vehicle vehicle = vehicle("vehicle-1", "OLD-123");
        VehicleRequest request = new VehicleRequest(
            "driver-2", "Honda", "Civic", "NEW-456", 5, "Sedan");
        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(vehicle));
        allowValidRequest(request);
        when(vehicleRepository.existsByVehiclePlateIgnoreCaseAndIdNot("NEW-456", "vehicle-1"))
                .thenReturn(false);
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);

        Vehicle result = vehicleService.updateVehicle("vehicle-1", request);

        assertEquals("driver-2", result.getDriverId());
        assertEquals("Honda", result.getVehicleMake());
        assertEquals("Civic", result.getVehicleModel());
        assertEquals("NEW-456", result.getVehiclePlate());
        assertEquals(5, result.getVehicleCapacity());
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void updateVehicle_rejectsPlateUsedByAnotherVehicle() {
        Vehicle vehicle = vehicle("vehicle-1", "OLD-123");
        VehicleRequest request = new VehicleRequest(
            "driver-1", "Toyota", "Camry", "DUPLICATE", 4, "Sedan");
        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(vehicle));
        allowValidRequest(request);
        when(vehicleRepository.existsByVehiclePlateIgnoreCaseAndIdNot("DUPLICATE", "vehicle-1"))
                .thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> vehicleService.updateVehicle("vehicle-1", request));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void deleteVehicle_deletesExistingVehicle() {
        Vehicle vehicle = vehicle("vehicle-1", "ABC-123");
        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(vehicle));

        vehicleService.deleteVehicle("vehicle-1");

        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void deleteVehicle_throwsWhenVehicleDoesNotExist() {
        when(vehicleRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> vehicleService.deleteVehicle("missing"));
        verify(vehicleRepository, never()).delete(any(Vehicle.class));
    }

    private void allowValidRequest(VehicleRequest request) {
        when(validator.validate(request)).thenReturn(Set.of());
    }

    private static VehicleRequest validRequest() {
        return new VehicleRequest("driver-1", "Toyota", "Camry", "ABC-123", 4, "Sedan");
    }

    private static Vehicle vehicle(String id, String plate) {
        return new Vehicle(id, "driver-1", "Toyota", "Camry", plate, 4, "Sedan");
    }
}