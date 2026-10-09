package com.ridelink.ride.dto;

import com.ridelink.ride.model.LocationPoint;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {
    private String id;
    private String passengerId;
    private String driverId;
    private String driverAccountId;
    private LocationPoint pickupLocation;
    private LocationPoint destinationLocation;
    private String serviceArea;
    private String vehicleType;
    private RideStatus status;
    private Double estimatedDistanceKm;
    private Integer estimatedDurationMinutes;
    private Double estimatedFare;
    private Double actualDistanceKm;
    private Integer actualDurationMinutes;
    private Double finalFare;
    private String paymentStatus;
    private Instant requestedAt;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private Instant updatedAt;

    public static RideResponse from(Ride ride) {
        return RideResponse.builder()
                .id(ride.getId())
                .passengerId(ride.getPassengerId())
                .driverId(ride.getDriverId())
                .driverAccountId(ride.getDriverAccountId())
                .pickupLocation(ride.getPickupLocation())
                .destinationLocation(ride.getDestinationLocation())
                .serviceArea(ride.getServiceArea())
                .vehicleType(ride.getVehicleType())
                .status(ride.getStatus())
                .estimatedDistanceKm(ride.getEstimatedDistanceKm())
                .estimatedDurationMinutes(ride.getEstimatedDurationMinutes())
                .estimatedFare(ride.getEstimatedFare())
                .actualDistanceKm(ride.getActualDistanceKm())
                .actualDurationMinutes(ride.getActualDurationMinutes())
                .finalFare(ride.getFinalFare())
                .paymentStatus(ride.getPaymentStatus())
                .requestedAt(ride.getRequestedAt())
                .assignedAt(ride.getAssignedAt())
                .acceptedAt(ride.getAcceptedAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                .cancelledAt(ride.getCancelledAt())
                .cancellationReason(ride.getCancellationReason())
                .updatedAt(ride.getUpdatedAt())
                .build();
    }
}
