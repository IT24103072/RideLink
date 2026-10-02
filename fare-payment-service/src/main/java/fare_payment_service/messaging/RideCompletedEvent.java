package fare_payment_service.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/** Mirrors Ride Management Service's event shape exactly - the message contract between the two services. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideCompletedEvent implements Serializable {
    private String rideId;
    private String passengerId;
    private String driverId;
    private String pickupPlaceName;
    private String destinationPlaceName;
    private Instant completedAt;
}
