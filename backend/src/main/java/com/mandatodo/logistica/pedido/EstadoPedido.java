package com.mandatodo.logistica.pedido;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum EstadoPedido {
    PENDIENTE,
    EN_REVISION,
    EN_TRANSITO,
    CANCELADO_EN_TRANSITO,
    ENTREGADO,
    FALLIDO;

    @JsonCreator
    public static EstadoPedido desde(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim()
            .toUpperCase(Locale.ROOT)
            .replace(' ', '_');
        for (EstadoPedido estado : values()) {
            if (estado.name().equals(normalizado)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de pedido inválido: " + valor);
    }
}
