package com.ridelink.account.service;

import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.ApiException;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.model.UserAccount;
import com.ridelink.account.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AccountService {

    private final UserAccountRepository repository;

    public AccountService(UserAccountRepository repository) {
        this.repository = repository;
    }

    public UserResponse getProfile(String userId) {
        return UserResponse.from(findUserById(userId));
    }

    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        UserAccount user = findUserById(userId);
        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(request.phoneNumber());
        user.setUpdatedAt(Instant.now());
        return UserResponse.from(repository.save(user));
    }

    public UserResponse changeRole(String userId, Role newRole, String actorUserId) {
        if (!isAdmin(actorUserId)) {
            throw ApiException.forbidden("Only administrators can change user roles");
        }
        if (newRole == null || newRole == Role.ADMIN) {
            throw ApiException.badRequest("Role must be PASSENGER or DRIVER");
        }

        UserAccount user = findUserById(userId);
        user.setRole(newRole);
        user.setUpdatedAt(Instant.now());
        return UserResponse.from(repository.save(user));
    }

    public UserResponse changeStatus(String userId, AccountStatus newStatus, String actorUserId) {
        if (!isAdmin(actorUserId)) {
            throw ApiException.forbidden("Only administrators can change account status");
        }
        if (newStatus == null) {
            throw ApiException.badRequest("Account status is required");
        }

        UserAccount user = findUserById(userId);
        user.setStatus(newStatus);
        user.setUpdatedAt(Instant.now());
        return UserResponse.from(repository.save(user));
    }

    public List<UserResponse> listAccounts(String actorUserId) {
        if (!isAdmin(actorUserId)) {
            throw ApiException.forbidden("Only administrators can list accounts");
        }
        return repository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    private UserAccount findUserById(String userId) {
        return repository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User account not found"));
    }

    private boolean isAdmin(String userId) {
        return repository.findById(userId)
                .map(UserAccount::getRole)
                .map(role -> role == Role.ADMIN)
                .orElse(false);
    }
}
