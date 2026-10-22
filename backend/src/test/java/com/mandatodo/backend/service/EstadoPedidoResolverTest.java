package com.mandatodo.backend.service;

import com.mandatodo.backend.model.EstadoPedido;
import com.mandatodo.backend.model.TipoCliente;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoPedidoResolverTest {

    private final EstadoPedidoResolver resolver = new EstadoPedidoResolver();

    @Test
    void clienteFrecuenteNuncaPasaARevision() {
        EstadoPedidoResolver.Resolucion resolucion = resolver.resolver(
                TipoCliente.Empresa,
                true,
                true,
                true,
                true
        );

        assertThat(resolucion.estado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(resolucion.observacion()).contains("frecuente");
    }

    @Test
    void direccionAmbiguaPasaARevision() {
        EstadoPedidoResolver.Resolucion resolucion = resolver.resolver(
                TipoCliente.Particular,
                false,
                true,
                false,
                false
        );

        assertThat(resolucion.estado()).isEqualTo(EstadoPedido.EN_REVISION);
    }

    @Test
    void empresaNoFrecuenteConDuplicadoPasaARevision() {
        EstadoPedidoResolver.Resolucion resolucion = resolver.resolver(
                TipoCliente.Empresa,
                false,
                false,
                false,
                true
        );

        assertThat(resolucion.estado()).isEqualTo(EstadoPedido.EN_REVISION);
        assertThat(resolucion.observacion()).contains("duplicado");
    }

    @Test
    void particularDuplicadoNoGeneraRetencionComercial() {
        EstadoPedidoResolver.Resolucion resolucion = resolver.resolver(
                TipoCliente.Particular,
                false,
                false,
                false,
                true
        );

        assertThat(resolucion.estado()).isEqualTo(EstadoPedido.PENDIENTE);
    }

    @Test
    void pedidoValidoIngresaPendiente() {
        EstadoPedidoResolver.Resolucion resolucion = resolver.resolver(
                TipoCliente.Empresa,
                false,
                false,
                false,
                false
        );

        assertThat(resolucion.estado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(resolucion.observacion()).isNull();
    }
}
