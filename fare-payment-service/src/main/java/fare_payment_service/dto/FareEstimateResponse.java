package fare_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class FareEstimateResponse {
    private BigDecimal estimatedFare;
    private String formula;
}
