package com.mandatodo.logistica.pedido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cancelación de un pedido que ya salió a la calle
 * (POST /api/pedidos/{id}/cancelar-transito).
 */
public record CancelarTransitoRequest(
    @NotBlank(message = "es obligatorio")
    @Size(max = 255, message = "no puede superar los 255 caracteres")
    String motivoCancelacion,

    // Opcional: en producción el operario se toma del JWT.
    Long operarioId
) {
}
