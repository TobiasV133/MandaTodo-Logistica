package com.mandatodo.backend.dto.auth;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        OperarioResponse operario
) {
}
