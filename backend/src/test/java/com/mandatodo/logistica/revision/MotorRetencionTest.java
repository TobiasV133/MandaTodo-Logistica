package com.mandatodo.logistica.revision;

import static org.assertj.core.api.Assertions.assertThat;

import com.mandatodo.logistica.pedido.FiabilidadGeocodificacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * El duplicado del día ya no se evalúa en el motor: lo resuelve
 * {@code PedidoService#validarYGuardarPedido(Pedido)} contra la base.
 */
class MotorRetencionTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 10, 23, 10, 0);

    private final MotorRetencion motor = new MotorRetencion();

    @Test
    void noRetieneUnPedidoParticularConDireccionCompleta() {
        PedidoEvaluable nuevo = pedido(
            "Caja de electrónica",
            "10.00",
            "5000.00",
            "Av. Corrientes",
            1234,
            "4 B",
            "CABA"
        );

        ResultadoRetencion resultado = motor.evaluar(
            new EvaluacionRetencion(nuevo, FiabilidadGeocodificacion.ALTA)
        );

        assertThat(resultado.retenido()).isFalse();
        assertThat(resultado.motivos()).isEmpty();
        assertThat(resultado.alertas()).isEmpty();
    }

    @Test
    void retieneAlturaCero() {
        PedidoEvaluable nuevo = pedido(
            "Caja de electrónica",
            "10.00",
            "5000.00",
            "Av. Corrientes",
            0,
            null,
            "CABA"
        );

        ResultadoRetencion resultado = motor.evaluar(
            new EvaluacionRetencion(nuevo, FiabilidadGeocodificacion.ALTA)
        );

        assertThat(resultado.retenido()).isTrue();
        assertThat(resultado.motivos()).containsExactly(MotivoRetencion.DIRECCION_AMBIGUA);
        assertThat(resultado.alertas()).containsExactly(AlertaRetencion.ALTURA_CERO);
    }

    @Test
    void retieneCalleSinNumero() {
        PedidoEvaluable nuevo = pedido(
            "Carga paletizada",
            "25.00",
            "25000.00",
            "S/N",
            0,
            null,
            "San Miguel"
        );

        ResultadoRetencion resultado = motor.evaluar(
            new EvaluacionRetencion(nuevo, FiabilidadGeocodificacion.ALTA)
        );

        assertThat(resultado.retenido()).isTrue();
        assertThat(resultado.motivos()).containsExactly(MotivoRetencion.DIRECCION_AMBIGUA);
        assertThat(resultado.alertas())
            .containsExactlyInAnyOrder(AlertaRetencion.ALTURA_CERO, AlertaRetencion.SIN_NUMERO);
    }

    @Test
    void retieneCuandoFaltaLaLocalidad() {
        PedidoEvaluable nuevo = pedido(
            "Caja de electrónica",
            "10.00",
            "5000.00",
            "Av. Corrientes",
            1234,
            null,
            " "
        );

        ResultadoRetencion resultado = motor.evaluar(
            new EvaluacionRetencion(nuevo, FiabilidadGeocodificacion.ALTA)
        );

        assertThat(resultado.retenido()).isTrue();
        assertThat(resultado.motivos()).containsExactly(MotivoRetencion.DIRECCION_AMBIGUA);
        assertThat(resultado.alertas()).containsExactly(AlertaRetencion.GEO_AMBIGUA);
    }

    @Test
    void retieneCuandoLaGeocodificacionEsDeFiabilidadBaja() {
        PedidoEvaluable nuevo = pedido(
            "Caja de electrónica",
            "10.00",
            "5000.00",
            "Av. Corrientes",
            1234,
            null,
            "CABA"
        );

        ResultadoRetencion resultado = motor.evaluar(
            new EvaluacionRetencion(nuevo, FiabilidadGeocodificacion.BAJA)
        );

        assertThat(resultado.retenido()).isTrue();
        assertThat(resultado.alertas()).containsExactly(AlertaRetencion.FIABILIDAD_BAJA);
    }

    @Test
    void retieneCuandoLaGeocodificacionEsDeFiabilidadMedia() {
        PedidoEvaluable nuevo = pedido(
            "Caja de electrónica",
            "10.00",
            "5000.00",
            "Av. Corrientes",
            1234,
            null,
            "CABA"
        );

        ResultadoRetencion resultado = motor.evaluar(
            new EvaluacionRetencion(nuevo, FiabilidadGeocodificacion.MEDIA)
        );

        assertThat(resultado.retenido()).isTrue();
        assertThat(resultado.alertas()).containsExactly(AlertaRetencion.FIABILIDAD_MEDIA);
    }

    private PedidoEvaluable pedido(
        String descripcion,
        String pesoKg,
        String montoEnvio,
        String calle,
        int altura,
        String pisoDepto,
        String localidad
    ) {
        return new PedidoEvaluable(
            descripcion,
            new BigDecimal(pesoKg),
            new BigDecimal(montoEnvio),
            calle,
            altura,
            pisoDepto,
            localidad,
            HOY
        );
    }
}
