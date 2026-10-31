package com.mandatodo.logistica.tarifa.dto;

import com.mandatodo.logistica.tarifa.TarifaValores;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Valores editables del tarifario. Solo un administrador puede aplicarlos.
 */
public record ActualizarTarifaRequest(
    @NotNull(message = "La tarifa base es obligatoria")
    @DecimalMin(value = "0.00", message = "La tarifa base no puede ser negativa")
    @Digits(integer = 10, fraction = 2, message = "La tarifa base admite hasta 2 decimales")
    BigDecimal tarifaBase,

    @NotNull(message = "La tarifa media es obligatoria")
    @DecimalMin(value = "0.00", message = "La tarifa media no puede ser negativa")
    @Digits(integer = 10, fraction = 2, message = "La tarifa media admite hasta 2 decimales")
    BigDecimal tarifaMedia,

    @NotNull(message = "El recargo por km es obligatorio")
    @DecimalMin(value = "0.00", message = "El recargo por km no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El recargo por km admite hasta 2 decimales")
    BigDecimal recargoPorKm,

    @NotNull(message = "El recargo por kg es obligatorio")
    @DecimalMin(value = "0.00", message = "El recargo por kg no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El recargo por kg admite hasta 2 decimales")
    BigDecimal recargoPorKg,

    @NotNull(message = "El límite de peso del nivel base es obligatorio")
    @DecimalMin(value = "0.001", message = "El límite de peso del nivel base debe ser mayor a 0")
    @Digits(integer = 7, fraction = 3, message = "El límite de peso admite hasta 3 decimales")
    BigDecimal limitePesoBase,

    @NotNull(message = "El límite de kilómetros del nivel base es obligatorio")
    @DecimalMin(value = "0.001", message = "El límite de kilómetros del nivel base debe ser mayor a 0")
    @Digits(integer = 7, fraction = 3, message = "El límite de kilómetros admite hasta 3 decimales")
    BigDecimal limiteKmBase,

    @NotNull(message = "El límite de peso del nivel medio es obligatorio")
    @DecimalMin(value = "0.001", message = "El límite de peso del nivel medio debe ser mayor a 0")
    @Digits(integer = 7, fraction = 3, message = "El límite de peso admite hasta 3 decimales")
    BigDecimal limitePesoMedio,

    @NotNull(message = "El límite de kilómetros del nivel medio es obligatorio")
    @DecimalMin(value = "0.001", message = "El límite de kilómetros del nivel medio debe ser mayor a 0")
    @Digits(integer = 7, fraction = 3, message = "El límite de kilómetros admite hasta 3 decimales")
    BigDecimal limiteKmMedio,

    @NotNull(message = "El peso máximo es obligatorio")
    @DecimalMin(value = "0.001", message = "El peso máximo debe ser mayor a 0")
    @Digits(integer = 7, fraction = 3, message = "El peso máximo admite hasta 3 decimales")
    BigDecimal maxPesoKg,

    @NotNull(message = "El máximo de kilómetros es obligatorio")
    @DecimalMin(value = "0.001", message = "El máximo de kilómetros debe ser mayor a 0")
    @Digits(integer = 7, fraction = 3, message = "El máximo de kilómetros admite hasta 3 decimales")
    BigDecimal maxKilometros
) {

    public TarifaValores aValores() {
        return new TarifaValores(
            tarifaBase,
            tarifaMedia,
            recargoPorKm,
            recargoPorKg,
            limitePesoBase,
            limiteKmBase,
            limitePesoMedio,
            limiteKmMedio,
            maxPesoKg,
            maxKilometros
        );
    }
}
