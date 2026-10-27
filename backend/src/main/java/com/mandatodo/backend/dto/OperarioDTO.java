package com.mandatodo.backend.dto;

import com.mandatodo.backend.model.EstadoOperario;
import com.mandatodo.backend.model.Operario;
import com.mandatodo.backend.model.Rol;

/**
 * Datos de salida de un operario.
 */
public record OperarioDTO(
        Integer idOperario,
        String username,
        String nombre,
        String email,
        Rol rol,
        EstadoOperario estado
) {

    public static OperarioDTO from(Operario operario) {
        return new OperarioDTO(
                operario.getIdOperario(),
                operario.getUsername(),
                operario.getNombre(),
                operario.getEmail(),
                operario.getRol(),
                operario.getEstado()
        );
    }
}
