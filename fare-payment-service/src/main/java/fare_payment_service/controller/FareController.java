package fare_payment_service.controller;

import fare_payment_service.dto.FareEstimateResponse;
import fare_payment_service.service.FareCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fares")
@RequiredArgsConstructor
@Tag(name = "Fares", description = "Fare estimation - public, called server-to-server by Ride Service")
public class FareController {

    private final FareCalculationService fareCalculationService;

    @GetMapping("/estimate")
    @Operation(summary = "Get a fare estimate for a pickup/destination pair")
    public FareEstimateResponse estimate(@RequestParam String pickup, @RequestParam String destination) {
        var amount = fareCalculationService.estimate(pickup, destination);
        return new FareEstimateResponse(amount, "baseFare + (distanceKm * perKmRate) + (durationMin * perMinRate)");
    }
}
