package com.mandatodo.backend.dto.auth;

import com.mandatodo.backend.model.Operario;
import com.mandatodo.backend.model.Rol;

public record OperarioResponse(
        Integer idOperario,
        String username,
        String nombreCompleto,
        String email,
        Rol rol
) {
    public static OperarioResponse from(Operario operario) {
        return new OperarioResponse(
                operario.getIdOperario(),
                operario.getUsername(),
                operario.getNombreCompleto(),
                operario.getEmail(),
                operario.getRol()
        );
    }
}
