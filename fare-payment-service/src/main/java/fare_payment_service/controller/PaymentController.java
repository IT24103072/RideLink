package fare_payment_service.controller;

import fare_payment_service.dto.PaymentResponse;
import fare_payment_service.dto.RecordPaymentRequest;
import fare_payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Simulated payment recording, status, and receipts")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Record a simulated payment for a completed ride (method=FAIL simulates a failure)")
    public PaymentResponse recordPayment(@Valid @RequestBody RecordPaymentRequest request) {
        return paymentService.recordPayment(request);
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get payment status for a ride")
    public PaymentResponse getByRideId(@PathVariable String rideId) {
        return paymentService.getByRideId(rideId);
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Get a receipt for a completed payment")
    public PaymentResponse getReceipt(@PathVariable String id) {
        return paymentService.getReceipt(id);
    }
}
