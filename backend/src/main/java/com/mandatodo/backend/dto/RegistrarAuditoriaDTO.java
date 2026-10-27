package com.mandatodo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para registrar un evento de auditoría.
 * El operario responsable se obtiene de la sesión autenticada.
 */
public record RegistrarAuditoriaDTO(

        @NotBlank(message = "La actividad es obligatoria")
        @Size(max = 500, message = "La actividad no puede superar los 500 caracteres")
        String actividad,

        @NotBlank(message = "El tipo de evento es obligatorio")
        String tipoEvento,

        Integer idPedido
) {
}
