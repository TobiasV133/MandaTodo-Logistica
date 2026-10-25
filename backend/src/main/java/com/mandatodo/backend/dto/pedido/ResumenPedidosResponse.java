package com.mandatodo.backend.dto.pedido;

public record ResumenPedidosResponse(
        long pendientes,
        long enRevision,
        long entregados,
        long clientes
) {
}
