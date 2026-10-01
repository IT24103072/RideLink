package com.ridelink.driver_vehicle_service.controller;

import com.ridelink.driver_vehicle_service.dto.DriverRequest;
import com.ridelink.driver_vehicle_service.dto.DriverServiceAreaUpdateRequest;
import com.ridelink.driver_vehicle_service.dto.DriverStatusUpdateRequest;
import com.ridelink.driver_vehicle_service.dto.DriverUpdateRequest;
import com.ridelink.driver_vehicle_service.dto.AvailableDriverVehicleDto;
import com.ridelink.driver_vehicle_service.model.Driver;
import com.ridelink.driver_vehicle_service.service.DriverService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/drivers")
@OpenAPIDefinition(
    info = @Info(title = "RideLink - Driver & Vehicle Service", version = "1.0"),
    tags = {
        @Tag(name = "Driver Management"),
        @Tag(name = "Vehicle Management")
    })
@Tag(name = "Driver Management")
@SecurityRequirements({
    @SecurityRequirement(name = "userRoleHeader"),
    @SecurityRequirement(name = "userIdHeader")
})
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Register a driver")
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Driver registered"),
            @ApiResponse(responseCode = "400", description = "Invalid driver request"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed"),
            @ApiResponse(responseCode = "409", description = "License number already exists")
        })
        @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    public Driver registerDriver(@Valid @RequestBody DriverRequest request) {
        return driverService.registerDriver(request);
    }

    @GetMapping("/{id}")
        @Operation(summary = "Get a driver by ID")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver returned"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Driver ID does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
        })
        @PreAuthorize("hasRole('ADMIN') or (hasRole('DRIVER') and #id == authentication.name)")
        public Driver getDriver(@PathVariable("id") String id) {
        return driverService.getDriver(id);
    }

    @PutMapping("/{id}")
        @Operation(summary = "Update a driver's profile")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver profile updated"),
            @ApiResponse(responseCode = "400", description = "Invalid driver request"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Driver ID does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Driver not found"),
            @ApiResponse(responseCode = "409", description = "License number already exists")
        })
            @PreAuthorize("hasRole('ADMIN') or (hasRole('DRIVER') and #id == authentication.name)")
    public Driver updateDriver(
                @PathVariable("id") String id,
            @Valid @RequestBody DriverUpdateRequest request) {
        return driverService.updateDriver(id, request);
    }

    @PutMapping("/{id}/service-area")
        @Operation(summary = "Update a driver's service area")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service area updated"),
            @ApiResponse(responseCode = "400", description = "Invalid service area"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Driver ID does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
        })
            @PreAuthorize("hasRole('ADMIN') or (hasRole('DRIVER') and #id == authentication.name)")
    public Driver updateDriverServiceArea(
                @PathVariable("id") String id,
            @Valid @RequestBody DriverServiceAreaUpdateRequest request) {
        return driverService.updateDriverServiceArea(id, request.getServiceArea());
    }

    @PutMapping("/{id}/status")
        @Operation(summary = "Update a driver's account status")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
        })
            @PreAuthorize("hasRole('ADMIN')")
    public Driver updateDriverStatus(
                @PathVariable("id") String id,
            @Valid @RequestBody DriverStatusUpdateRequest request) {
        return driverService.updateDriverStatus(id, request.getStatus());
    }

    @PatchMapping("/{id}/availability")
        @Operation(summary = "Update a driver's availability")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability updated"),
            @ApiResponse(responseCode = "400", description = "Invalid availability"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Driver ID does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
        })
        @PreAuthorize("hasRole('ADMIN') or (hasRole('DRIVER') and #id == authentication.name)")
        public Driver updateAvailability(@PathVariable("id") String id, @RequestParam String status) {
        return driverService.updateAvailability(id, status);
    }

    @PatchMapping("/{id}/location")
        @Operation(summary = "Update a driver's current location")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location updated"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Driver ID does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
        })
            @PreAuthorize("hasRole('ADMIN') or (hasRole('DRIVER') and #id == authentication.name)")
    public Driver updateLocation(
                @PathVariable("id") String id,
            @RequestParam Double lat,
            @RequestParam Double lng) {
        return driverService.updateLocation(id, lat, lng);
    }

    @GetMapping("/available")
        @Operation(summary = "Find available drivers with registered vehicles")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching drivers returned"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Service or admin role required")
        })
        @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    public List<AvailableDriverVehicleDto> getAvailableDrivers(
            @RequestParam(required = false) String serviceArea,
            @RequestParam(required = false) String vehicleType) {
        return driverService.getAvailableDrivers(serviceArea, vehicleType);
    }
}