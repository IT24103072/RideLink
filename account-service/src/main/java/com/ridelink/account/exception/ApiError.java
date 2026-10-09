package com.ridelink.account.exception;

import java.time.Instant;
import java.util.List;

/** Consistent JSON error body returned by every failing request. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
}
