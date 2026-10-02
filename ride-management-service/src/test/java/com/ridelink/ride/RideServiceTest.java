package com.ridelink.ride;

import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.ApiException;
import com.ridelink.ride.model.LocationPoint;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock private RideRepository rideRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private RideService rideService;

    private static final String DRIVER_SERVICE_URL = "http://localhost:8082";
    private static final String FARE_SERVICE_URL    = "http://localhost:8084";

    // Setup URLs via reflection (simulating @Value injection)
    void injectUrls() {
        ReflectionTestUtils.setField(rideService, "driverServiceUrl", DRIVER_SERVICE_URL);
        ReflectionTestUtils.setField(rideService, "fareServiceUrl", FARE_SERVICE_URL);
    }

    
    // requestRide Tests
    
    @Test
    void requestRide_noDriverAvailable_statusRemainedRequested() {
        injectUrls();
        String passengerId = UUID.randomUUID().toString();
        CreateRideRequest req = buildCreateRequest();

        // Fare service fails → fallback
        when(restTemplate.postForObject(anyString(), any(), eq(com.ridelink.ride.dto.FareDtos.EstimateResponse.class)))
                .thenThrow(new RuntimeException("fare-service down"));
        // Driver service returns empty array
        when(restTemplate.getForObject(anyString(), eq(com.ridelink.ride.dto.DriverDto[].class)))
                .thenReturn(new com.ridelink.ride.dto.DriverDto[]{});

        Ride savedRide = buildRide(passengerId, null, RideStatus.REQUESTED);
        when(rideRepository.save(any())).thenReturn(savedRide);

        RideResponse response = rideService.requestRide(passengerId, req);

        assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
        assertThat(response.getDriverId()).isNull();
    }

    @Test
    void requestRide_driverAvailable_statusBecomesAssigned() {
        injectUrls();
        String passengerId = UUID.randomUUID().toString();
        String driverId = UUID.randomUUID().toString();
        CreateRideRequest req = buildCreateRequest();

        com.ridelink.ride.dto.DriverDto driverDto = new com.ridelink.ride.dto.DriverDto();
        driverDto.setId(driverId);
        driverDto.setAccountId(UUID.randomUUID().toString());

        when(restTemplate.postForObject(anyString(), any(), eq(com.ridelink.ride.dto.FareDtos.EstimateResponse.class)))
                .thenThrow(new RuntimeException("fare down"));
        when(restTemplate.getForObject(anyString(), eq(com.ridelink.ride.dto.DriverDto[].class)))
                .thenReturn(new com.ridelink.ride.dto.DriverDto[]{driverDto});

        Ride savedRide = buildRide(passengerId, driverId, RideStatus.ASSIGNED);
        when(rideRepository.save(any())).thenReturn(savedRide);

        RideResponse response = rideService.requestRide(passengerId, req);

        assertThat(response.getStatus()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(response.getDriverId()).isEqualTo(driverId);
    }

    
    // acceptRide Tests
    

    @Test
    void acceptRide_fromAssigned_succeeds() {
        injectUrls();
        String rideId = UUID.randomUUID().toString();
        String driverAccountId = UUID.randomUUID().toString();
        Ride ride = buildRide("pass-1", "drv-1", RideStatus.ASSIGNED);
        ride.setId(rideId);
        ride.setDriverAccountId(driverAccountId);

        when(rideRepository.findByBusinessId(rideId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RideResponse resp = rideService.acceptRide(rideId, driverAccountId);

        assertThat(resp.getStatus()).isEqualTo(RideStatus.ACCEPTED);
        assertThat(resp.getAcceptedAt()).isNotNull();
    }

    @Test
    void acceptRide_wrongStatus_throwsConflict() {
        injectUrls();
        String rideId = UUID.randomUUID().toString();
        String driverAccountId = UUID.randomUUID().toString();
        Ride ride = buildRide("pass-1", "drv-1", RideStatus.REQUESTED); // not ASSIGNED
        ride.setId(rideId);
        ride.setDriverAccountId(driverAccountId);

        when(rideRepository.findByBusinessId(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.acceptRide(rideId, driverAccountId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Expected ride status ASSIGNED");
    }

    @Test
    void acceptRide_wrongDriver_throwsForbidden() {
        injectUrls();
        String rideId = UUID.randomUUID().toString();
        Ride ride = buildRide("pass-1", "drv-1", RideStatus.ASSIGNED);
        ride.setId(rideId);
        ride.setDriverAccountId("correct-account-id");

        when(rideRepository.findByBusinessId(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.acceptRide(rideId, "wrong-account-id"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not the assigned driver");
    }

    
    // cancelRide Tests
    

    @Test
    void cancelRide_fromRequested_succeeds() {
        injectUrls();
        String rideId = UUID.randomUUID().toString();
        Ride ride = buildRide("pass-1", null, RideStatus.REQUESTED);
        ride.setId(rideId);

        when(rideRepository.findByBusinessId(rideId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RideResponse resp = rideService.cancelRide(rideId, "pass-1", "Changed my mind");

        assertThat(resp.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(resp.getCancellationReason()).isEqualTo("Changed my mind");
    }

    @Test
    void cancelRide_fromInProgress_throwsConflict() {
        injectUrls();
        String rideId = UUID.randomUUID().toString();
        Ride ride = buildRide("pass-1", "drv-1", RideStatus.IN_PROGRESS);
        ride.setId(rideId);

        when(rideRepository.findByBusinessId(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.cancelRide(rideId, "pass-1", "reason"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already in progress");
    }

    
    // Queries
    

    @Test
    void getRidesByPassenger_returnsFilteredList() {
        injectUrls();
        String passengerId = UUID.randomUUID().toString();
        List<Ride> rides = List.of(
                buildRide(passengerId, null, RideStatus.COMPLETED),
                buildRide(passengerId, null, RideStatus.CANCELLED)
        );
        when(rideRepository.findByPassengerId(passengerId)).thenReturn(rides);

        List<RideResponse> responses = rideService.getRidesByPassenger(passengerId);

        assertThat(responses).hasSize(2);
    }

    
    // Helpers
    

    private CreateRideRequest buildCreateRequest() {
        return CreateRideRequest.builder()
                .pickupAddress("Colombo Fort")
                .pickupLatitude(6.9271)
                .pickupLongitude(79.8612)
                .destinationAddress("Kandy City Centre")
                .destinationLatitude(7.2906)
                .destinationLongitude(80.6337)
                .serviceArea("Colombo")
                .vehicleType("SEDAN")
                .estimatedDistanceKm(80.0)
                .build();
    }

    private Ride buildRide(String passengerId, String driverId, RideStatus status) {
        Instant now = Instant.now();
        return Ride.builder()
                .id(UUID.randomUUID().toString())
                .passengerId(passengerId)
                .driverId(driverId)
                .pickupLocation(LocationPoint.builder()
                        .address("Colombo Fort").latitude(6.9271).longitude(79.8612).build())
                .destinationLocation(LocationPoint.builder()
                        .address("Kandy").latitude(7.2906).longitude(80.6337).build())
                .serviceArea("Colombo")
                .vehicleType("SEDAN")
                .estimatedDistanceKm(80.0)
                .estimatedDurationMinutes(120)
                .estimatedFare(5000.0)
                .paymentStatus("PENDING")
                .status(status)
                .requestedAt(now)
                .startedAt(status == RideStatus.IN_PROGRESS || status == RideStatus.COMPLETED ? now : null)
                .updatedAt(now)
                .build();
    }
}
