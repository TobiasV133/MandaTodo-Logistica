package com.mandatodo.logistica.pedido.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CambioEstadoResponse(
    PedidoResponse pedido,
    boolean reprogramado,
    BigDecimal recargoAplicado,
    LocalDate fechaReprogramada
) {
}
