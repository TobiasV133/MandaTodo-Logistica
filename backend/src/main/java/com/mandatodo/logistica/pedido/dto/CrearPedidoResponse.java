package com.mandatodo.logistica.pedido.dto;

import com.mandatodo.logistica.revision.ResultadoRetencion;

public record CrearPedidoResponse(
    PedidoResponse pedido,
    ResultadoRetencion retencion
) {
}
