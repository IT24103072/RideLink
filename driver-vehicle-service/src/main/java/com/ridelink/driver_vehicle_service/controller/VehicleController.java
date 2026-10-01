package com.ridelink.driver_vehicle_service.controller;

import com.ridelink.driver_vehicle_service.dto.VehicleRequest;
import com.ridelink.driver_vehicle_service.model.Vehicle;
import com.ridelink.driver_vehicle_service.service.VehicleService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@Tag(name = "Vehicle Management")
@SecurityRequirements({
    @SecurityRequirement(name = "userRoleHeader"),
    @SecurityRequirement(name = "userIdHeader")
})
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Register a vehicle for a driver")
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehicle registered"),
            @ApiResponse(responseCode = "400", description = "Invalid vehicle request"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Vehicle must belong to this driver"),
            @ApiResponse(responseCode = "409", description = "Plate number already exists")
        })
        @PreAuthorize("hasRole('ADMIN') or hasRole('DRIVER')")
    public Vehicle registerVehicle(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody VehicleRequest request) {
        requireOwnVehicle(role, userId, request.getDriverId());
        return vehicleService.registerVehicle(request);
    }

    @GetMapping("/{id}")
        @Operation(summary = "Get a vehicle by ID")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle returned"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Vehicle does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
        })
        @PreAuthorize("hasRole('ADMIN') or hasRole('DRIVER')")
    public Vehicle getVehicle(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id) {
        Vehicle vehicle = vehicleService.getVehicle(id);
        requireOwnVehicle(role, userId, vehicle.getDriverId());
        return vehicle;
    }

    @GetMapping("/driver/{driverId}")
        @Operation(summary = "List vehicles for a driver")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicles returned"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Driver ID does not belong to user")
        })
        @PreAuthorize("hasRole('ADMIN') or (hasRole('DRIVER') and #driverId == authentication.name)")
        public List<Vehicle> getVehiclesByDriver(@PathVariable("driverId") String driverId) {
        return vehicleService.getVehiclesByDriver(driverId);
    }

    @PutMapping("/{id}")
        @Operation(summary = "Update a vehicle")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle updated"),
            @ApiResponse(responseCode = "400", description = "Invalid vehicle request"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Vehicle must remain with this driver"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "409", description = "Plate number already exists")
        })
        @PreAuthorize("hasRole('ADMIN') or hasRole('DRIVER')")
    public Vehicle updateVehicle(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id,
            @Valid @RequestBody VehicleRequest request) {
        if (!"ADMIN".equals(role)) {
            requireOwnVehicle(role, userId, vehicleService.getVehicle(id).getDriverId());
            requireOwnVehicle(role, userId, request.getDriverId());
        }
        return vehicleService.updateVehicle(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
        @Operation(summary = "Delete a vehicle")
        @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehicle deleted"),
            @ApiResponse(responseCode = "401", description = "Missing role or user ID header"),
            @ApiResponse(responseCode = "403", description = "Vehicle does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
        })
        @PreAuthorize("hasRole('ADMIN') or hasRole('DRIVER')")
    public void deleteVehicle(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id) {
        requireOwnVehicle(role, userId, vehicleService.getVehicle(id).getDriverId());
        vehicleService.deleteVehicle(id);
    }

    private void requireOwnVehicle(String role, String userId, String driverId) {
        if (!"ADMIN".equals(role)
                && (!"DRIVER".equals(role) || !userId.equals(driverId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
