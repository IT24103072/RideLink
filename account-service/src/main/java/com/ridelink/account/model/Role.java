package com.ridelink.account.model;

/**
 * Roles issued in the JWT "role" claim. Values are plain (no ROLE_ prefix);
 * the other services add the prefix themselves.
 */
public enum Role {
    PASSENGER,
    DRIVER,
    ADMIN
}
