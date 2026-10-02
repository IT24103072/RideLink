package fare_payment_service.service;

import fare_payment_service.dto.PaymentResponse;
import fare_payment_service.dto.RecordPaymentRequest;
import fare_payment_service.exception.ApiException;
import fare_payment_service.messaging.RideCompletedEvent;
import fare_payment_service.model.Payment;
import fare_payment_service.model.PaymentStatus;
import fare_payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FareCalculationService fareCalculationService;

    /**
     * Consumes the ride.completed event published by Ride Management
     * Service. This is the asynchronous interservice interaction: Fare
     * Service calculates the final fare and creates a PENDING payment
     * without Ride Service having to wait for it, and without Ride Service
     * needing to know anything about payments.
     */
    @RabbitListener(queues = "${app.rabbitmq.ride-completed-queue}")
    public void onRideCompleted(RideCompletedEvent event) {
        if (paymentRepository.existsByRideId(event.getRideId())) {
            log.warn("Payment already exists for ride {}, ignoring duplicate event", event.getRideId());
            return;
        }

        var finalFare = fareCalculationService.estimate(event.getPickupPlaceName(), event.getDestinationPlaceName());

        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .rideId(event.getRideId())
                .passengerId(event.getPassengerId())
                .driverId(event.getDriverId())
                .amount(finalFare)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        paymentRepository.save(payment);
        log.info("Created pending payment {} for ride {} with amount {}", payment.getId(), event.getRideId(), finalFare);
    }

    /**
     * Simulated payment recording - a passenger/admin calls this to mark a
     * pending payment as PAID. No real payment gateway is involved, per the
     * brief's "all payments must be simulated" requirement.
     */
    public PaymentResponse recordPayment(RecordPaymentRequest request) {
        Payment payment = paymentRepository.findByRideId(request.getRideId())
                .orElseThrow(() -> new ApiException(
                        "No pending payment found for ride " + request.getRideId() + " - has the ride been completed yet?",
                        HttpStatus.NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ApiException("This ride has already been paid for", HttpStatus.CONFLICT);
        }

        // Simulated failure case: negative scenario for the demo.
        if ("FAIL".equalsIgnoreCase(request.getMethod())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new ApiException("Simulated payment failure for testing", HttpStatus.PAYMENT_REQUIRED);
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        payment.setReceiptNumber("RCPT-" + payment.getId().substring(0, 8).toUpperCase());
        paymentRepository.save(payment);

        return PaymentResponse.from(payment);
    }

    public PaymentResponse getByRideId(String rideId) {
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new ApiException("No payment found for ride " + rideId, HttpStatus.NOT_FOUND));
        return PaymentResponse.from(payment);
    }

    public PaymentResponse getReceipt(String paymentId) {
        Payment payment = paymentRepository.findByIdIs(paymentId)
                .orElseThrow(() -> new ApiException("Payment not found: " + paymentId, HttpStatus.NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ApiException("Receipt not available - payment is not yet completed", HttpStatus.CONFLICT);
        }
        return PaymentResponse.from(payment);
    }
}
