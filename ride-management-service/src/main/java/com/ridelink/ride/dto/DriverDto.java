package com.ridelink.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDto {
    private String id;
    private String accountId;
    private String fullName;
    private String phoneNumber;
    private String licenseNumber;
    private String serviceArea;
    private String status;
    private Object vehicle;
    private Object currentLocation;
    private Double rating;
}
