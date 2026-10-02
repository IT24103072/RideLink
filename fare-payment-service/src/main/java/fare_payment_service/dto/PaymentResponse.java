package fare_payment_service.dto;

import fare_payment_service.model.Payment;
import fare_payment_service.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class PaymentResponse {
    private String id;
    private String rideId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String receiptNumber;
    private Instant paidAt;

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getRideId(), p.getAmount(), p.getStatus(), p.getReceiptNumber(), p.getPaidAt());
    }
}
