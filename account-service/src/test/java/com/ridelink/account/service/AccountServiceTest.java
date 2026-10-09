package com.ridelink.account.service;

import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.ApiException;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.model.UserAccount;
import com.ridelink.account.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserAccountRepository repository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldUpdateProfileForCurrentUser() {
        UserAccount user = UserAccount.builder()
                .id("user-1")
                .email("alice@example.com")
                .fullName("Alice Smith")
                .phoneNumber("0771234567")
                .role(Role.PASSENGER)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(repository.findById("user-1")).thenReturn(Optional.of(user));
        when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse updated = accountService.updateProfile("user-1", new UpdateProfileRequest("Alice Jones", "+94771234567"));

        assertEquals("Alice Jones", updated.fullName());
        assertEquals("+94771234567", updated.phoneNumber());
        assertEquals(Role.PASSENGER, updated.role());
    }

    @Test
    void shouldRejectStatusChangeForNonAdmin() {
        UserAccount user = UserAccount.builder()
                .id("user-2")
                .email("bob@example.com")
                .fullName("Bob")
                .phoneNumber("0760000000")
                .role(Role.DRIVER)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(repository.findById("user-3")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> accountService.changeStatus("user-2", AccountStatus.SUSPENDED, "user-3"));

        assertEquals("Only administrators can change account status", ex.getMessage());
    }
}
