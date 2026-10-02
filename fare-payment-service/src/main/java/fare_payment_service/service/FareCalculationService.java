package fare_payment_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Documented fare calculation rule:
 *   fare = baseFare + (distanceKm * perKmRate) + (durationMin * perMinRate)
 *
 * Since the brief allows simulated locations/coordinates (no real maps
 * integration required), distance and duration are simulated here using a
 * simple deterministic hash of the pickup/destination place names. This
 * keeps the SAME pickup/destination pair always producing the SAME
 * estimate, which is important for demoing consistent behaviour, while
 * avoiding any real geocoding dependency.
 */
@Service
public class FareCalculationService {

    private final BigDecimal baseFare;
    private final BigDecimal perKmRate;
    private final BigDecimal perMinRate;

    public FareCalculationService(@Value("${app.fare.base-fare}") String baseFare,
                                   @Value("${app.fare.per-km-rate}") String perKmRate,
                                   @Value("${app.fare.per-min-rate}") String perMinRate) {
        this.baseFare = new BigDecimal(baseFare);
        this.perKmRate = new BigDecimal(perKmRate);
        this.perMinRate = new BigDecimal(perMinRate);
    }

    public BigDecimal estimate(String pickupPlaceName, String destinationPlaceName) {
        double distanceKm = simulateDistanceKm(pickupPlaceName, destinationPlaceName);
        double durationMin = distanceKm * 2.2; // simple simulated speed assumption

        BigDecimal distanceCost = perKmRate.multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal durationCost = perMinRate.multiply(BigDecimal.valueOf(durationMin));

        return baseFare.add(distanceCost).add(durationCost).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Deterministic simulated distance: same pair of place names -> same
     * "distance" every time, bounded to a realistic 1-25km range.
     */
    private double simulateDistanceKm(String pickup, String destination) {
        int hash = Objects.hash(pickup.toLowerCase().trim(), destination.toLowerCase().trim());
        int bounded = Math.abs(hash % 2400); // 0-2399
        return 1.0 + (bounded / 100.0); // 1.0km - 24.99km
    }
}
