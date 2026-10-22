package com.mandatodo.backend.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tarifario único del módulo de carga.
 * El frontend replica estos valores en src/utils/tarifas.js para la previsualización.
 *
 * precioBase = cargo fijo + (km * valorKm) + (kg * valorKg)
 */
@Service
public class TarifaService {

    public static final BigDecimal CARGO_FIJO = new BigDecimal("2500.00");
    public static final BigDecimal VALOR_POR_KM = new BigDecimal("450.00");
    public static final BigDecimal VALOR_POR_KG = new BigDecimal("180.00");

    public Tarifa calcular(BigDecimal pesoKg, BigDecimal kilometros) {
        BigDecimal base = CARGO_FIJO
                .add(kilometros.multiply(VALOR_POR_KM))
                .add(pesoKg.multiply(VALOR_POR_KG))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal recargo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = base.add(recargo).setScale(2, RoundingMode.HALF_UP);

        return new Tarifa(base, recargo, total);
    }

    public record Tarifa(BigDecimal precioBase, BigDecimal recargo, BigDecimal precioTotal) {
    }
}
