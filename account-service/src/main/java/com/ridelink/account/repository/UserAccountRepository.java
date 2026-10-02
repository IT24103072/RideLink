package com.ridelink.account.repository;

import com.ridelink.account.model.Role;
import com.ridelink.account.model.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends MongoRepository<UserAccount, String> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    List<UserAccount> findByRole(Role role);
}
