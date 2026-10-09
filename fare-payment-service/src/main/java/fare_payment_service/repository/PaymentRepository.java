package fare_payment_service.repository;

import fare_payment_service.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByRideId(String rideId);
    Optional<Payment> findByIdIs(String id);
    boolean existsByRideId(String rideId);
}
