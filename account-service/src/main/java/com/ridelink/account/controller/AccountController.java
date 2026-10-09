package com.ridelink.account.controller;

import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.ApiException;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/account")
@Tag(name = "Account management", description = "Profile, role and status management")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user profile")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(accountService.getProfile(authentication.getName()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the current authenticated user profile")
    public ResponseEntity<UserResponse> updateCurrentUser(Authentication authentication,
                                                         @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(accountService.updateProfile(authentication.getName(), request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user profile by id; self or admin access")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId, Authentication authentication) {
        if (!userId.equals(authentication.getName()) && !isAdmin(authentication)) {
            throw ApiException.forbidden("You can only view your own profile unless you are an administrator");
        }
        return ResponseEntity.ok(accountService.getProfile(userId));
    }

    @GetMapping
    @Operation(summary = "List all accounts; administrator only")
    public ResponseEntity<List<UserResponse>> listAccounts(Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw ApiException.forbidden("Only administrators can list account records");
        }
        return ResponseEntity.ok(accountService.listAccounts(authentication.getName()));
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "Change a user role; administrator only")
    public ResponseEntity<UserResponse> changeRole(@PathVariable String userId,
                                                  @RequestParam Role role,
                                                  Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw ApiException.forbidden("Only administrators can change user roles");
        }
        return ResponseEntity.ok(accountService.changeRole(userId, role, authentication.getName()));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Change a user account status; administrator only")
    public ResponseEntity<UserResponse> changeStatus(@PathVariable String userId,
                                                    @RequestParam AccountStatus status,
                                                    Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw ApiException.forbidden("Only administrators can change account status");
        }
        return ResponseEntity.ok(accountService.changeStatus(userId, status, authentication.getName()));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
