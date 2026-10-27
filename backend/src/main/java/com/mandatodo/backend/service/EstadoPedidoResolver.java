package com.mandatodo.backend.service;

import com.mandatodo.backend.model.EstadoPedido;
import com.mandatodo.backend.model.MotivoRetencion;
import com.mandatodo.backend.model.TipoCliente;
import org.springframework.stereotype.Component;

/**
 * Reglas de retención del módulo de carga.
 *
 * <p>1. Cliente frecuente: nunca ingresa a EN_REVISION.
 * 2. Dirección ambigua o incompleta: EN_REVISION.
 * 3. Empresa no frecuente con pedido idéntico el mismo día: EN_REVISION.
 * 4. Particular duplicado: no genera retención comercial.</p>
 */
@Component
public class EstadoPedidoResolver {

    public Resolucion resolver(
            TipoCliente tipoCliente,
            boolean esFrecuente,
            boolean direccionAmbigua,
            boolean direccionIncompleta,
            boolean duplicadoComercialDelDia
    ) {
        if (esFrecuente) {
            return new Resolucion(
                    EstadoPedido.PENDIENTE,
                    "Cliente frecuente: el pedido ingresa directo a PENDIENTE.",
                    null
            );
        }

        if (direccionAmbigua || direccionIncompleta) {
            return new Resolucion(
                    EstadoPedido.EN_REVISION,
                    "Dirección ambigua o incompleta: requiere revisión manual antes de despachar.",
                    MotivoRetencion.DIRECCION_AMBIGUA
            );
        }

        if (tipoCliente == TipoCliente.Empresa && duplicadoComercialDelDia) {
            return new Resolucion(
                    EstadoPedido.EN_REVISION,
                    "Posible duplicado comercial del día: requiere revisión manual.",
                    MotivoRetencion.POSIBLE_DUPLICADO_COMERCIAL
            );
        }

        return new Resolucion(EstadoPedido.PENDIENTE, null, null);
    }

    public record Resolucion(
            EstadoPedido estado,
            String observacion,
            MotivoRetencion motivoRetencion
    ) {
    }
}
