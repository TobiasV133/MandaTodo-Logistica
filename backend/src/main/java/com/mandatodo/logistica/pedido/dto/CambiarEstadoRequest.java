package com.mandatodo.logistica.pedido.dto;

import com.mandatodo.logistica.pedido.EstadoPedido;
import com.mandatodo.logistica.pedido.MotivoFalla;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarEstadoRequest(
    @NotNull(message = "es obligatorio")
    EstadoPedido estado,

    MotivoFalla motivoFalla,

    @Size(max = 500, message = "no puede superar los 500 caracteres")
    String notaFalla,

    @Size(max = 255, message = "no puede superar los 255 caracteres")
    String motivoCancelacion,

    // Opcional: en producción el operario se toma del JWT.
    Long operarioId
) {
}
