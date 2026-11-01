package com.mandatodo.logistica.revision;

import com.mandatodo.logistica.pedido.FiabilidadGeocodificacion;

public record EvaluacionRetencion(
    PedidoEvaluable pedido,
    FiabilidadGeocodificacion fiabilidad
) {
}
