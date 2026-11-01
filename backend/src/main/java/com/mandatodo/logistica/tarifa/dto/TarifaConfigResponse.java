package com.mandatodo.logistica.tarifa.dto;

import com.mandatodo.logistica.tarifa.TarifaConfig;
import com.mandatodo.logistica.tarifa.TarifaValores;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TarifaConfigResponse(
    BigDecimal tarifaBase,
    BigDecimal tarifaMedia,
    BigDecimal recargoPorKm,
    BigDecimal recargoPorKg,
    BigDecimal limitePesoBase,
    BigDecimal limiteKmBase,
    BigDecimal limitePesoMedio,
    BigDecimal limiteKmMedio,
    BigDecimal maxPesoKg,
    BigDecimal maxKilometros,
    Long idOperarioUltimaModificacion,
    LocalDateTime fechaUltimaModificacion
) {

    public static TarifaConfigResponse porDefecto() {
        return desde(TarifaValores.POR_DEFECTO, null, null);
    }

    public static TarifaConfigResponse desde(TarifaConfig config) {
        return desde(
            config.aValores(),
            config.getIdOperarioUltimaModificacion(),
            config.getFechaUltimaModificacion()
        );
    }

    private static TarifaConfigResponse desde(
        TarifaValores valores,
        Long idOperario,
        LocalDateTime fechaModificacion
    ) {
        return new TarifaConfigResponse(
            valores.tarifaBase(),
            valores.tarifaMedia(),
            valores.recargoPorKm(),
            valores.recargoPorKg(),
            valores.limitePesoBase(),
            valores.limiteKmBase(),
            valores.limitePesoMedio(),
            valores.limiteKmMedio(),
            valores.maxPesoKg(),
            valores.maxKilometros(),
            idOperario,
            fechaModificacion
        );
    }
}
