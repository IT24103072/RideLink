package fare_payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecordPaymentRequest {
    @NotBlank
    private String rideId;

    /** Simulated payment method - "CASH", "CARD", "WALLET", etc. No real payment gateway integration. */
    @NotBlank
    private String method;
}
