package com.ridelink.ride.service;

import com.ridelink.ride.config.RabbitConfig;
import com.ridelink.ride.dto.*;
import com.ridelink.ride.exception.ApiException;
import com.ridelink.ride.model.LocationPoint;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    @Value("${app.services.driver-service-url}")
    private String driverServiceUrl;

    @Value("${app.services.fare-service-url}")
    private String fareServiceUrl;

    
    // Passenger request a ride
    
    public RideResponse requestRide(String passengerId, CreateRideRequest req) {
        Instant now = Instant.now();

        // 1. Estimate fare from fare-payment-service
        double distanceKm = req.getEstimatedDistanceKm() != null ? req.getEstimatedDistanceKm()
                : estimateHaversineKm(req.getPickupLatitude(), req.getPickupLongitude(),
                        req.getDestinationLatitude(), req.getDestinationLongitude());

        FareDtos.EstimateResponse fareEstimate = null;
        try {
            FareDtos.EstimateRequest fareReq = FareDtos.EstimateRequest.builder()
                    .pickupAddress(req.getPickupAddress())
                    .pickupLatitude(req.getPickupLatitude())
                    .pickupLongitude(req.getPickupLongitude())
                    .destinationAddress(req.getDestinationAddress())
                    .destinationLatitude(req.getDestinationLatitude())
                    .destinationLongitude(req.getDestinationLongitude())
                    .vehicleType(req.getVehicleType())
                    .directDistanceKm(distanceKm)
                    .build();
            fareEstimate = restTemplate.postForObject(
                    fareServiceUrl + "/api/fares/estimate", fareReq, FareDtos.EstimateResponse.class);
        } catch (Exception e) {
            log.warn("Fare service unavailable during ride request, using fallback estimate: {}", e.getMessage());
        }

        double estimatedFare = fareEstimate != null ? fareEstimate.getTotalEstimatedFare() : distanceKm * 50.0;
        int estimatedMinutes = fareEstimate != null ? fareEstimate.getEstimatedDurationMinutes() : (int) (distanceKm * 3);

        // 2. Find an available driver in the service area
        DriverDto assignedDriver = null;
        try {
            String url = driverServiceUrl + "/api/drivers/available?serviceArea=" + req.getServiceArea()
                    + "&vehicleType=" + req.getVehicleType();
            DriverDto[] drivers = restTemplate.getForObject(url, DriverDto[].class);
            if (drivers != null && drivers.length > 0) {
                assignedDriver = drivers[0]; // simple first-match dispatch
            }
        } catch (Exception e) {
            log.warn("Driver service unavailable during dispatch, ride will remain REQUESTED: {}", e.getMessage());
        }

        // 3. Build and persist the ride
        Ride.RideBuilder builder = Ride.builder()
                .id(UUID.randomUUID().toString())
                .passengerId(passengerId)
                .pickupLocation(LocationPoint.builder()
                        .address(req.getPickupAddress())
                        .latitude(req.getPickupLatitude())
                        .longitude(req.getPickupLongitude())
                        .build())
                .destinationLocation(LocationPoint.builder()
                        .address(req.getDestinationAddress())
                        .latitude(req.getDestinationLatitude())
                        .longitude(req.getDestinationLongitude())
                        .build())
                .serviceArea(req.getServiceArea())
                .vehicleType(req.getVehicleType())
                .estimatedDistanceKm(distanceKm)
                .estimatedDurationMinutes(estimatedMinutes)
                .estimatedFare(estimatedFare)
                .paymentStatus("PENDING")
                .requestedAt(now)
                .updatedAt(now);

        if (assignedDriver != null) {
            builder.driverId(assignedDriver.getId())
                    .driverAccountId(assignedDriver.getAccountId())
                    .status(RideStatus.ASSIGNED)
                    .assignedAt(now);

            // Mark driver as ON_TRIP via driver service
            try {
                restTemplate.patchForObject(
                        driverServiceUrl + "/api/drivers/" + assignedDriver.getId() + "/availability?status=ON_TRIP",
                        null, Void.class);
            } catch (Exception e) {
                log.warn("Failed to update driver {} status to ON_TRIP: {}", assignedDriver.getId(), e.getMessage());
            }
        } else {
            builder.status(RideStatus.REQUESTED);
        }

        Ride saved = rideRepository.save(builder.build());

        // 4. Publish ride.requested event for async listeners
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.RIDE_EXCHANGE,
                    RabbitConfig.RIDE_REQUESTED_ROUTING_KEY,
                    Map.of("rideId", saved.getId(), "passengerId", passengerId,
                            "status", saved.getStatus().name()));
        } catch (Exception e) {
            log.warn("Failed to publish ride.requested event: {}", e.getMessage());
        }

        return RideResponse.from(saved);
    }

    
    // Driver accept assigned ride
    
    public RideResponse acceptRide(String rideId, String driverAccountId) {
        Ride ride = getActiveRide(rideId);
        requireStatus(ride, RideStatus.ASSIGNED);
        requireDriverOwnership(ride, driverAccountId);

        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        return RideResponse.from(rideRepository.save(ride));
    }

    
    // Driver start the trip
    
    public RideResponse startRide(String rideId, String driverAccountId) {
        Ride ride = getActiveRide(rideId);
        requireStatus(ride, RideStatus.ACCEPTED);
        requireDriverOwnership(ride, driverAccountId);

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        return RideResponse.from(rideRepository.save(ride));
    }

    
    // Driver: complete the trip
    
    public RideResponse completeRide(String rideId, String driverAccountId) {
        Ride ride = getActiveRide(rideId);
        requireStatus(ride, RideStatus.IN_PROGRESS);
        requireDriverOwnership(ride, driverAccountId);

        Instant now = Instant.now();
        long durationSeconds = now.getEpochSecond() - ride.getStartedAt().getEpochSecond();
        int durationMinutes = Math.max(1, (int) (durationSeconds / 60));
        double actualDistanceKm = ride.getEstimatedDistanceKm() != null ? ride.getEstimatedDistanceKm() : 5.0;

        // Calculate final fare
        double finalFare = ride.getEstimatedFare();
        try {
            FareDtos.CalculateRequest calcReq = FareDtos.CalculateRequest.builder()
                    .rideId(rideId)
                    .vehicleType(ride.getVehicleType())
                    .actualDistanceKm(actualDistanceKm)
                    .actualDurationMinutes(durationMinutes)
                    .surgeMultiplier(1.0)
                    .build();
            FareDtos.CalculateResponse calcResp = restTemplate.postForObject(
                    fareServiceUrl + "/api/fares/calculate", calcReq, FareDtos.CalculateResponse.class);
            if (calcResp != null) finalFare = calcResp.getFinalFare();
        } catch (Exception e) {
            log.warn("Fare service unavailable during completion, using estimated fare: {}", e.getMessage());
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(now);
        ride.setUpdatedAt(now);
        ride.setActualDistanceKm(actualDistanceKm);
        ride.setActualDurationMinutes(durationMinutes);
        ride.setFinalFare(finalFare);
        ride.setPaymentStatus("COMPLETED");

        Ride saved = rideRepository.save(ride);

        // Mark driver AVAILABLE again
        if (ride.getDriverId() != null) {
            try {
                restTemplate.patchForObject(
                        driverServiceUrl + "/api/drivers/" + ride.getDriverId() + "/availability?status=AVAILABLE",
                        null, Void.class);
                // increment trip count
                restTemplate.postForObject(
                        driverServiceUrl + "/api/drivers/" + ride.getDriverId() + "/trips/increment",
                        null, Void.class);
            } catch (Exception e) {
                log.warn("Failed to update driver status after completion: {}", e.getMessage());
            }
        }

        // Publish ride-completed event
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.RIDE_EXCHANGE,
                    RabbitConfig.RIDE_COMPLETED_ROUTING_KEY,
                    Map.of("rideId", saved.getId(),
                            "passengerId", saved.getPassengerId(),
                            "driverId", saved.getDriverId() != null ? saved.getDriverId() : "",
                            "finalFare", saved.getFinalFare()));
        } catch (Exception e) {
            log.warn("Failed to publish ride.completed event: {}", e.getMessage());
        }

        return RideResponse.from(saved);
    }

    
    // Cancel ride (passenger or driver)
    
    public RideResponse cancelRide(String rideId, String cancellerAccountId, String reason) {
        Ride ride = getActiveRide(rideId);

        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new ApiException("Ride is already " + ride.getStatus().name().toLowerCase(), HttpStatus.CONFLICT);
        }
        if (ride.getStatus() == RideStatus.IN_PROGRESS) {
            throw new ApiException("Cannot cancel a ride that is already in progress", HttpStatus.CONFLICT);
        }

        Instant now = Instant.now();
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledAt(now);
        ride.setCancellationReason(reason);
        ride.setUpdatedAt(now);

        // Free up driver if assigned
        if (ride.getDriverId() != null) {
            try {
                restTemplate.patchForObject(
                        driverServiceUrl + "/api/drivers/" + ride.getDriverId() + "/availability?status=AVAILABLE",
                        null, Void.class);
            } catch (Exception e) {
                log.warn("Failed to free driver {} after cancellation: {}", ride.getDriverId(), e.getMessage());
            }
        }

        return RideResponse.from(rideRepository.save(ride));
    }

    
    // Queries
    
    public RideResponse getRideById(String rideId) {
        return RideResponse.from(getActiveRide(rideId));
    }

    public List<RideResponse> getRidesByPassenger(String passengerId) {
        return rideRepository.findByPassengerId(passengerId).stream()
                .map(RideResponse::from)
                .collect(Collectors.toList());
    }

    public List<RideResponse> getRidesByDriver(String driverAccountId) {
        // Look up the driver profile to get driverId
        String driverId = null;
        try {
            DriverDto driver = restTemplate.getForObject(
                    driverServiceUrl + "/api/drivers/account/" + driverAccountId, DriverDto.class);
            if (driver != null) driverId = driver.getId();
        } catch (Exception e) {
            log.warn("Could not resolve driverId for account {}: {}", driverAccountId, e.getMessage());
        }

        if (driverId == null) return List.of();
        String finalDriverId = driverId;
        return rideRepository.findByDriverId(finalDriverId).stream()
                .map(RideResponse::from)
                .collect(Collectors.toList());
    }

    public List<RideResponse> getRidesByStatus(RideStatus status) {
        return rideRepository.findByStatus(status).stream()
                .map(RideResponse::from)
                .collect(Collectors.toList());
    }

    public List<RideResponse> getAllRides() {
        return rideRepository.findAll().stream()
                .map(RideResponse::from)
                .collect(Collectors.toList());
    }

    
    // Helpers
    
    private Ride getActiveRide(String rideId) {
        return rideRepository.findByBusinessId(rideId)
                .orElseThrow(() -> new ApiException("Ride not found: " + rideId, HttpStatus.NOT_FOUND));
    }

    private void requireStatus(Ride ride, RideStatus expected) {
        if (ride.getStatus() != expected) {
            throw new ApiException(
                    "Expected ride status " + expected + " but was " + ride.getStatus(),
                    HttpStatus.CONFLICT);
        }
    }

    private void requireDriverOwnership(Ride ride, String driverAccountId) {
        if (!driverAccountId.equals(ride.getDriverAccountId())) {
            throw new ApiException("You are not the assigned driver for this ride", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Haversine formula – straight-line distance in km.
     */
    private double estimateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
