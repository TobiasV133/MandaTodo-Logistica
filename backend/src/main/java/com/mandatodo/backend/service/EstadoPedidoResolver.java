package com.mandatodo.backend.service;

import com.mandatodo.backend.model.EstadoPedido;
import com.mandatodo.backend.model.TipoCliente;
import org.springframework.stereotype.Component;

/**
 * Reglas de retención del módulo de carga.
 *
 * 1. Cliente frecuente: NUNCA ingresa a EN_REVISION, pasa directo a PENDIENTE.
 * 2. Dirección ambigua, incompleta o S/N: EN_REVISION.
 * 3. Empresa no frecuente con pedido idéntico el mismo día: EN_REVISION.
 * 4. Particular duplicado: no genera retención comercial (PENDIENTE).
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
            return new Resolucion(EstadoPedido.PENDIENTE, "Cliente frecuente: el pedido ingresa directo a PENDIENTE.");
        }

        if (direccionAmbigua || direccionIncompleta) {
            return new Resolucion(
                    EstadoPedido.EN_REVISION,
                    "Dirección ambigua o incompleta: requiere revisión manual antes de despachar."
            );
        }

        if (tipoCliente == TipoCliente.Empresa && duplicadoComercialDelDia) {
            return new Resolucion(
                    EstadoPedido.EN_REVISION,
                    "Posible duplicado comercial del día: requiere revisión manual."
            );
        }

        return new Resolucion(EstadoPedido.PENDIENTE, null);
    }

    public record Resolucion(EstadoPedido estado, String observacion) {
    }
}
