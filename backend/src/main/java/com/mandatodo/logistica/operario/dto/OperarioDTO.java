package com.mandatodo.logistica.operario.dto;

import com.mandatodo.logistica.operario.EstadoOperario;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.RolOperario;

public record OperarioDTO(
    Long idOperario,
    String username,
    String nombre,
    String email,
    RolOperario rol,
    EstadoOperario estado
) {
    public static OperarioDTO desde(Operario operario) {
        return new OperarioDTO(
            operario.getId(),
            operario.getUsername(),
            operario.getNombre(),
            operario.getEmail(),
            operario.getRol(),
            operario.getEstado()
        );
    }
}
