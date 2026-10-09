package com.ridelink.ride.controller;

import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
@Tag(name = "Ride Management", description = "Ride lifecycle: request, assign, accept, start, complete, cancel")
public class RideController {

    private final RideService rideService;

    
    // Health probe
    
    @GetMapping("/health")
    @Operation(summary = "Health probe (no auth required)")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ride-management-service OK");
    }

    
    // Passenger: Request a ride
    
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('ADMIN')")
    @Operation(summary = "Passenger requests a new ride – triggers driver dispatch and fare estimation")
    public ResponseEntity<RideResponse> requestRide(
            Authentication authentication,
            @Valid @RequestBody CreateRideRequest request) {
        String passengerId = (String) authentication.getPrincipal();
        RideResponse response = rideService.requestRide(passengerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    
    // Get ride by ID (passenger, driver, admin)
    
    @GetMapping("/{rideId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get ride details by ID")
    public ResponseEntity<RideResponse> getRide(@PathVariable String rideId) {
        return ResponseEntity.ok(rideService.getRideById(rideId));
    }

    
    // Passenger: my rides
    
    @GetMapping("/my/passenger")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('ADMIN')")
    @Operation(summary = "Get all rides for the authenticated passenger")
    public ResponseEntity<List<RideResponse>> myPassengerRides(Authentication authentication) {
        String passengerId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(rideService.getRidesByPassenger(passengerId));
    }

    
    // Driver: my rides
    
    @GetMapping("/my/driver")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    @Operation(summary = "Get all rides assigned to the authenticated driver")
    public ResponseEntity<List<RideResponse>> myDriverRides(Authentication authentication) {
        String driverAccountId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(rideService.getRidesByDriver(driverAccountId));
    }

    
    // Driver: accept assigned ride
    
    @PostMapping("/{rideId}/accept")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver accepts an ASSIGNED ride")
    public ResponseEntity<RideResponse> acceptRide(
            @PathVariable String rideId,
            Authentication authentication) {
        String driverAccountId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(rideService.acceptRide(rideId, driverAccountId));
    }

    
    // Driver: start trip
    
    @PostMapping("/{rideId}/start")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver starts an ACCEPTED ride (passenger picked up)")
    public ResponseEntity<RideResponse> startRide(
            @PathVariable String rideId,
            Authentication authentication) {
        String driverAccountId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(rideService.startRide(rideId, driverAccountId));
    }

    // ──────────────────────────────────────────
    // Driver: complete trip
    // ──────────────────────────────────────────
    @PostMapping("/{rideId}/complete")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver completes an IN_PROGRESS ride – final fare calculated and driver freed")
    public ResponseEntity<RideResponse> completeRide(
            @PathVariable String rideId,
            Authentication authentication) {
        String driverAccountId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(rideService.completeRide(rideId, driverAccountId));
    }

    
    // Cancel ride (passenger or driver)
    
    @PostMapping("/{rideId}/cancel")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER') or hasRole('ADMIN')")
    @Operation(summary = "Cancel a REQUESTED or ASSIGNED ride")
    public ResponseEntity<RideResponse> cancelRide(
            @PathVariable String rideId,
            Authentication authentication,
            @RequestBody(required = false) CancelRideRequest cancelRequest) {
        String accountId = (String) authentication.getPrincipal();
        String reason = cancelRequest != null ? cancelRequest.getReason() : "No reason provided";
        return ResponseEntity.ok(rideService.cancelRide(rideId, accountId, reason));
    }

    
    // Admin: rides by status
    
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: list all rides, optionally filter by status")
    public ResponseEntity<List<RideResponse>> getAllRides(
            @RequestParam(required = false) RideStatus status) {
        if (status != null) {
            return ResponseEntity.ok(rideService.getRidesByStatus(status));
        }
        return ResponseEntity.ok(rideService.getAllRides());
    }
}
