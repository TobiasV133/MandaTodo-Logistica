package com.mandatodo.backend.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        int status,
        String error,
        String message,
        List<String> details,
        LocalDateTime timestamp
) {
    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(status, error, message, details, LocalDateTime.now());
    }
}
