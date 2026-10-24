package com.mandatodo.logistica.pedido;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultadoReprogramacion(
    BigDecimal recargoAplicado,
    LocalDate fechaReprogramada,
    BigDecimal montoFinal,
    BigDecimal montoAnterior
) {
}
