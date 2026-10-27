package com.mandatodo.logistica.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * El campo se llama username por compatibilidad con el frontend, pero el
 * identificador de ingreso es el email del operario.
 */
public record LoginRequest(
    @NotBlank(message = "El email es obligatorio") String username,
    @NotBlank(message = "La contraseña es obligatoria") String password
) {
}
