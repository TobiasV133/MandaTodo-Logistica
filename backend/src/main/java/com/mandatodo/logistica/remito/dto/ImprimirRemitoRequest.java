package com.mandatodo.logistica.remito.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos de la impresión de un remito.
 *
 * <p>{@code motivoReimpresion} es obligatorio a partir de la segunda impresión del mismo pedido.
 * {@code kilometros} es opcional porque el esquema MySQL no guarda los kilómetros del pedido: el
 * operario puede informarlos para que queden impresos en el documento.</p>
 */
public record ImprimirRemitoRequest(
    // Opcional: en producción el operario se toma del JWT.
    Long operarioId,

    @Size(max = 200, message = "no puede superar los 200 caracteres")
    String motivoReimpresion,

    @DecimalMin(value = "0.0", message = "no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "admite hasta 8 enteros y 2 decimales")
    BigDecimal kilometros
) {
}
