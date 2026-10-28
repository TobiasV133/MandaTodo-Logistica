package com.mandatodo.logistica.revision;

import com.mandatodo.logistica.pedido.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vista mínima de un pedido para que el motor de retención no dependa de JPA.
 */
public record PedidoEvaluable(
    String descripcion,
    BigDecimal pesoKg,
    BigDecimal montoEnvio,
    String calle,
    Integer altura,
    String pisoDepto,
    String localidad,
    LocalDateTime fechaCreacion
) {
    public static PedidoEvaluable desde(Pedido pedido) {
        return new PedidoEvaluable(
            pedido.getDescripcion(),
            pedido.getPesoKg(),
            pedido.getMontoEnvio(),
            pedido.getCalle(),
            pedido.getAltura(),
            pedido.getPisoDepto(),
            pedido.getLocalidad(),
            pedido.getFechaCreacion()
        );
    }
}
