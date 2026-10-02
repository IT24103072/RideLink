package com.ridelink.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FareDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstimateRequest {
        private String pickupAddress;
        private Double pickupLatitude;
        private Double pickupLongitude;
        private String destinationAddress;
        private Double destinationLatitude;
        private Double destinationLongitude;
        private String vehicleType;
        private Double directDistanceKm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstimateResponse {
        private Double estimatedDistanceKm;
        private Integer estimatedDurationMinutes;
        private Double baseFare;
        private Double distanceFare;
        private Double durationFare;
        private Double surgeMultiplier;
        private Double totalEstimatedFare;
        private String vehicleType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculateRequest {
        private String rideId;
        private String vehicleType;
        private Double actualDistanceKm;
        private Integer actualDurationMinutes;
        private Double surgeMultiplier;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculateResponse {
        private String rideId;
        private Double finalFare;
        private Double baseFare;
        private Double distanceFare;
        private Double durationFare;
        private Double taxAmount;
    }
}
