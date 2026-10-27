package com.mandatodo.logistica.auth.dto;

import com.mandatodo.logistica.operario.Operario;

public record OperarioResponse(
    Long idOperario,
    String username,
    String nombreCompleto,
    String email,
    String rol
) {
    public static OperarioResponse desde(Operario operario) {
        return new OperarioResponse(
            operario.getId(),
            operario.getEmail(),
            operario.getNombre(),
            operario.getEmail(),
            operario.getRol().name()
        );
    }
}
