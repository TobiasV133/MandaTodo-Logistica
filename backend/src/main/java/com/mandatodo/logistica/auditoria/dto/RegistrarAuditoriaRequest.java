package com.mandatodo.logistica.auditoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta manual de un evento de auditoría (POST /api/auditoria).
 * `tipoEvento` se recibe como texto para tolerar tanto los nombres del enum
 * (ALTA, CAMBIO_DE_ESTADO) como los valores de la UI (Alta, Cambio de Estado).
 */
public record RegistrarAuditoriaRequest(
    @NotNull(message = "es obligatorio")
    Long idOperario,

    Long idPedido,

    @NotBlank(message = "es obligatorio")
    @Size(max = 500, message = "no puede superar los 500 caracteres")
    String actividad,

    @NotBlank(message = "es obligatorio")
    String tipoEvento
) {
}
