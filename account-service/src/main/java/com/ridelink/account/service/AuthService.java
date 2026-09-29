package com.ridelink.account.service;

import com.ridelink.account.dto.AuthResponse;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.ApiException;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.model.UserAccount;
import com.ridelink.account.repository.UserAccountRepository;
import com.ridelink.account.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository repository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw ApiException.forbidden("Admin accounts cannot be created through registration");
        }
        String email = normalise(request.email());
        if (repository.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }

        Instant now = Instant.now();
        UserAccount user = UserAccount.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phoneNumber(request.phoneNumber())
                .role(request.role())
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            return UserResponse.from(repository.save(user));
        } catch (DuplicateKeyException ex) {
            // Two registrations with the same email raced past the existsByEmail check.
            throw ApiException.conflict("An account with this email already exists");
        }
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = repository.findByEmail(normalise(request.email())).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Same message for unknown email and wrong password, so emails cannot be probed.
            throw ApiException.unauthorized("Invalid email or password");
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("Account is not active");
        }
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getRole());
    }

    private String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
