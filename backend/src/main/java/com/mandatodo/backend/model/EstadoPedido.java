package com.mandatodo.backend.model;

/**
 * Estados posibles del ciclo de vida de un pedido (DER: tabla pedidos).
 */
public enum EstadoPedido {
    PENDIENTE,
    EN_REVISION,
    EN_TRANSITO,
    CANCELADO_EN_TRANSITO,
    ENTREGADO,
    FALLIDO
}
