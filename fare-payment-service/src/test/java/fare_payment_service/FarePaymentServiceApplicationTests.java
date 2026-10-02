package fare_payment_service;

import fare_payment_service.service.FareCalculationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FareCalculationServiceTest {

    private final FareCalculationService service = new FareCalculationService("300.00", "80.00", "15.00");

    @Test
    void estimate_isDeterministicForSamePickupAndDestination() {
        BigDecimal first = service.estimate("Kandy Fort", "Peradeniya");
        BigDecimal second = service.estimate("Kandy Fort", "Peradeniya");
        assertEquals(first, second);
    }

    @Test
    void estimate_isAtLeastTheBaseFare() {
        BigDecimal estimate = service.estimate("A", "B");
        assertTrue(estimate.compareTo(new BigDecimal("300.00")) >= 0);
    }

    @Test
    void estimate_isRoundedToTwoDecimalPlaces() {
        BigDecimal estimate = service.estimate("Kandy Fort", "Peradeniya");
        assertEquals(2, estimate.scale());
    }
}
