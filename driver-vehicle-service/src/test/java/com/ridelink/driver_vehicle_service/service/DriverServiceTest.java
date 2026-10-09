package com.ridelink.driver_vehicle_service.service;

import com.ridelink.driver_vehicle_service.dto.DriverRequest;
import com.ridelink.driver_vehicle_service.dto.DriverUpdateRequest;
import com.ridelink.driver_vehicle_service.model.Vehicle;
import com.ridelink.driver_vehicle_service.model.Driver;
import com.ridelink.driver_vehicle_service.repository.DriverRepository;
import com.ridelink.driver_vehicle_service.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverService driverService;

    @Test
    void registerDriver_savesDriverWithOfflineAvailability() {
        DriverRequest request = new DriverRequest(
                "account-1",
                "license-1",
                "Toyota",
                "Camry",
                "ABC-123",
                4,
                "North"
        );
        when(driverRepository.save(any(Driver.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Driver savedDriver = driverService.registerDriver(request);

        ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepository).save(driverCaptor.capture());
        assertEquals("OFFLINE", driverCaptor.getValue().getAvailabilityStatus());
        assertEquals("OFFLINE", savedDriver.getAvailabilityStatus());
        assertEquals("account-1", savedDriver.getAccountId());
    }

    @Test
    void registerDriver_rejectsDuplicateLicenseNumber() {
        DriverRequest request = new DriverRequest(
                "account-1",
                "license-1",
                "Toyota",
                "Camry",
                "ABC-123",
                4,
                "North"
        );
        when(driverRepository.existsByLicenseNumberIgnoreCase("license-1")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> driverService.registerDriver(request));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void getDriver_returnsDriverWhenFound() {
        Driver driver = new Driver();
        driver.setId("driver-1");
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));

        Driver result = driverService.getDriver("driver-1");

        assertEquals(driver, result);
        verify(driverRepository).findById("driver-1");
    }

    @Test
    void getDriver_throwsWhenDriverDoesNotExist() {
        when(driverRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> driverService.getDriver("missing"));
        verify(driverRepository).findById("missing");
    }

    @Test
    void updateDriver_updatesProfileFields() {
        Driver driver = new Driver();
        driver.setId("driver-1");
        driver.setLicenseNumber("license-1");
        DriverUpdateRequest request = new DriverUpdateRequest(
                "Asha Perera", "0771234567", "license-1", "Colombo");
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);

        Driver result = driverService.updateDriver("driver-1", request);

        assertEquals("Asha Perera", result.getName());
        assertEquals("0771234567", result.getContactNumber());
        assertEquals("license-1", result.getLicenseNumber());
        assertEquals("Colombo", result.getServiceArea());
        verify(driverRepository).save(driver);
    }

    @Test
    void updateDriver_rejectsLicenseUsedByAnotherDriver() {
        Driver driver = new Driver();
        driver.setId("driver-1");
        driver.setLicenseNumber("license-1");
        DriverUpdateRequest request = new DriverUpdateRequest(
                "Asha Perera", "0771234567", "license-2", "Colombo");
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot("license-2", "driver-1"))
                .thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> driverService.updateDriver("driver-1", request));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void updateDriverServiceArea_updatesArea() {
        Driver driver = new Driver();
        driver.setId("driver-1");
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);

        Driver result = driverService.updateDriverServiceArea("driver-1", "Colombo");

        assertEquals("Colombo", result.getServiceArea());
    }

    @Test
    void updateDriverStatus_updatesLifecycleStatus() {
        Driver driver = new Driver();
        driver.setId("driver-1");
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);

        Driver result = driverService.updateDriverStatus("driver-1", "SUSPENDED");

        assertEquals("SUSPENDED", result.getStatus());
    }

    @Test
    void updateDriverStatus_rejectsUnsupportedStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> driverService.updateDriverStatus("driver-1", "BUSY"));
        verify(driverRepository, never()).findById("driver-1");
    }

    @Test
    void updateAvailability_updatesAndUppercasesStatus() {
        Driver driver = new Driver();
        driver.setId("driver-1");
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);

        Driver result = driverService.updateAvailability("driver-1", "available");

        assertEquals("AVAILABLE", result.getAvailabilityStatus());
        verify(driverRepository).findById("driver-1");
        verify(driverRepository).save(driver);
    }

    @Test
    void updateAvailability_rejectsUnsupportedStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> driverService.updateAvailability("driver-1", "ON_TRIP"));
        verify(driverRepository, never()).findById("driver-1");
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void updateAvailability_throwsWhenDriverDoesNotExist() {
        when(driverRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> driverService.updateAvailability("missing", "available"));
        verify(driverRepository).findById("missing");
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void updateLocation_updatesLatitudeAndLongitude() {
        Driver driver = new Driver();
        driver.setId("driver-1");
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);

        Driver result = driverService.updateLocation("driver-1", 12.34, 56.78);

        assertEquals(12.34, result.getCurrentLat());
        assertEquals(56.78, result.getCurrentLng());
        verify(driverRepository).findById("driver-1");
        verify(driverRepository).save(driver);
    }

        @Test
        void updateLocation_rejectsCoordinatesOutsideValidRanges() {
        assertThrows(IllegalArgumentException.class,
            () -> driverService.updateLocation("driver-1", 90.01, 56.78));
        assertThrows(IllegalArgumentException.class,
            () -> driverService.updateLocation("driver-1", 12.34, -180.01));
        assertThrows(IllegalArgumentException.class,
            () -> driverService.updateLocation("driver-1", Double.NaN, 56.78));
        verify(driverRepository, never()).findById("driver-1");
        verify(driverRepository, never()).save(any(Driver.class));
        }

    @Test
    void getAvailableDrivers_filtersByServiceAreaAndVehicleType() {
        Driver driver = new Driver();
        driver.setId("driver-1");
        driver.setName("Asha");
        driver.setServiceArea("North");
        driver.setCurrentLat(6.92);
        driver.setCurrentLng(79.86);
        Vehicle vehicle = new Vehicle("vehicle-1", "driver-1", "Toyota", "Prius",
                "ABC-123", 4, "Hybrid");
        List<Driver> drivers = List.of(driver);
        when(driverRepository.findByAvailabilityStatusAndServiceArea("AVAILABLE", "North"))
                .thenReturn(drivers);
        Vehicle nonMatchingVehicle = new Vehicle("vehicle-2", "driver-1", "Honda", "Civic",
            "XYZ-789", 5, "Sedan");
        when(vehicleRepository.findByDriverId("driver-1"))
            .thenReturn(List.of(vehicle, nonMatchingVehicle));

        var result = driverService.getAvailableDrivers("North", "hybrid");

        assertEquals(1, result.size());
        assertEquals("driver-1", result.get(0).getDriverId());
        assertEquals("Asha", result.get(0).getName());
        assertEquals("North", result.get(0).getServiceArea());
        assertEquals(6.92, result.get(0).getLatitude());
        assertEquals(79.86, result.get(0).getLongitude());
        assertEquals("vehicle-1", result.get(0).getVehicleId());
        assertEquals("Hybrid", result.get(0).getVehicleType());
        assertEquals("ABC-123", result.get(0).getPlateNumber());
        assertEquals(4, result.get(0).getCapacity());
        verify(driverRepository).findByAvailabilityStatusAndServiceArea("AVAILABLE", "North");
        verify(driverRepository, never()).findByAvailabilityStatus("AVAILABLE");
    }

    @Test
    void getAvailableDrivers_excludesDriversWithoutVehicles() {
        Driver withVehicle = new Driver();
        withVehicle.setId("driver-1");
        Driver withoutVehicle = new Driver();
        withoutVehicle.setId("driver-2");
        List<Driver> drivers = List.of(withVehicle, withoutVehicle);
        when(driverRepository.findByAvailabilityStatus("AVAILABLE")).thenReturn(drivers);
        when(vehicleRepository.findByDriverId("driver-1")).thenReturn(List.of(
                new Vehicle("vehicle-1", "driver-1", "Toyota", "Prius",
                        "ABC-123", 4, "Hybrid")));
        when(vehicleRepository.findByDriverId("driver-2")).thenReturn(List.of());

        var result = driverService.getAvailableDrivers(null, null);

        assertEquals(1, result.size());
        assertEquals("driver-1", result.get(0).getDriverId());
        verify(driverRepository).findByAvailabilityStatus("AVAILABLE");
        verify(driverRepository, never())
                .findByAvailabilityStatusAndServiceArea(any(String.class), any(String.class));
    }

    @Test
    void getAvailableDrivers_returnsEmptyListWhenNoDriversAreAvailable() {
        when(driverRepository.findByAvailabilityStatus("AVAILABLE")).thenReturn(List.of());

        List<?> result = driverService.getAvailableDrivers(null, null);

        assertTrue(result.isEmpty());
        verify(driverRepository).findByAvailabilityStatus("AVAILABLE");
    }
}
