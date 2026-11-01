package com.mandatodo.logistica.tarifa;

import java.math.BigDecimal;

/**
 * Valores vigentes del tarifario escalonado de MandaTodo.
 *
 * <ul>
 *   <li>Nivel 1: hasta {@code limitePesoBase} kg y {@code limiteKmBase} km.</li>
 *   <li>Nivel 2: hasta {@code limitePesoMedio} kg y {@code limiteKmMedio} km.</li>
 *   <li>Nivel 3: supera el nivel medio y suma {@code recargoPorKm} por km extra y
 *       {@code recargoPorKg} por kg extra.</li>
 * </ul>
 *
 * <p>Los valores se pueden editar desde la pantalla de pedidos y quedan persistidos en
 * {@code tarifa_config}. Si la tabla no tiene fila, se usan los valores por defecto.</p>
 */
public record TarifaValores(
    BigDecimal tarifaBase,
    BigDecimal tarifaMedia,
    BigDecimal recargoPorKm,
    BigDecimal recargoPorKg,
    BigDecimal limitePesoBase,
    BigDecimal limiteKmBase,
    BigDecimal limitePesoMedio,
    BigDecimal limiteKmMedio,
    BigDecimal maxPesoKg,
    BigDecimal maxKilometros
) {

    public static final TarifaValores POR_DEFECTO = new TarifaValores(
        new BigDecimal("10000.00"),
        new BigDecimal("30000.00"),
        new BigDecimal("5000.00"),
        new BigDecimal("3000.00"),
        new BigDecimal("5.00"),
        new BigDecimal("5.00"),
        new BigDecimal("20.00"),
        new BigDecimal("15.00"),
        new BigDecimal("80.00"),
        new BigDecimal("30.00")
    );

    public String mensajeCapacidad() {
        return "El pedido excede la capacidad logística máxima ("
            + texto(maxPesoKg) + "kg / " + texto(maxKilometros) + "km)";
    }

    private static String texto(BigDecimal valor) {
        if (valor == null) {
            return "0";
        }
        return valor.stripTrailingZeros().toPlainString();
    }
}
