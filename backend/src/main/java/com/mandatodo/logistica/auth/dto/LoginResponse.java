package com.mandatodo.logistica.auth.dto;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresIn,
    OperarioResponse operario
) {
}
