package com.mandatodo.logistica.pedido.dto;

public record ResumenPedidosResponse(
    long pendientes,
    long enRevision,
    long entregados,
    long clientes
) {
}
