package fare_payment_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private String mongoId;

    private String id; // business UUID

    @Indexed(unique = true)
    private String rideId; // Ride Management Service's ride id

    private String passengerId;
    private String driverId;

    private BigDecimal amount;

    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private String receiptNumber;

    private Instant createdAt;
    private Instant paidAt;
}
