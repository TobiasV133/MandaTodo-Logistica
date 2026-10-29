package com.mandatodo.logistica.revision;

import com.mandatodo.logistica.cliente.TipoCliente;
import com.mandatodo.logistica.pedido.FiabilidadGeocodificacion;
import java.util.List;

public record EvaluacionRetencion(
    TipoCliente tipoCliente,
    PedidoEvaluable pedido,
    List<PedidoEvaluable> pedidosDelMismoDia,
    FiabilidadGeocodificacion fiabilidad
) {
    public EvaluacionRetencion {
        pedidosDelMismoDia = pedidosDelMismoDia == null ? List.of() : List.copyOf(pedidosDelMismoDia);
    }
}
