package com.ridelink.ride.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "rides")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

    @Id
    private String mongoId;

    @Indexed(unique = true)
    private String id; // business UUID

    @Indexed
    private String passengerId;

    @Indexed
    private String driverId;

    private String driverAccountId;

    private LocationPoint pickupLocation;

    private LocationPoint destinationLocation;

    private String serviceArea;

    private String vehicleType;

    @Indexed
    private RideStatus status;

    private Double estimatedDistanceKm;

    private Integer estimatedDurationMinutes;

    private Double estimatedFare;

    private Double actualDistanceKm;

    private Integer actualDurationMinutes;

    private Double finalFare;

    private String paymentStatus; // PENDING, COMPLETED, FAILED

    private Instant requestedAt;

    private Instant assignedAt;

    private Instant acceptedAt;

    private Instant startedAt;

    private Instant completedAt;

    private Instant cancelledAt;

    private String cancellationReason;

    private Instant updatedAt;
}
