package com.mandatodo.logistica.revision;

import static org.assertj.core.api.Assertions.assertThat;

import com.mandatodo.logistica.cliente.TipoCliente;
import com.mandatodo.logistica.pedido.FiabilidadGeocodificacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

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

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Particular,
            nuevo,
            List.of(),
            FiabilidadGeocodificacion.ALTA
        ));

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

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Particular,
            nuevo,
            List.of(),
            FiabilidadGeocodificacion.ALTA
        ));

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

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Empresa,
            nuevo,
            List.of(),
            FiabilidadGeocodificacion.ALTA
        ));

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

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Particular,
            nuevo,
            List.of(),
            FiabilidadGeocodificacion.ALTA
        ));

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

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Particular,
            nuevo,
            List.of(),
            FiabilidadGeocodificacion.BAJA
        ));

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

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Particular,
            nuevo,
            List.of(),
            FiabilidadGeocodificacion.MEDIA
        ));

        assertThat(resultado.retenido()).isTrue();
        assertThat(resultado.alertas()).containsExactly(AlertaRetencion.FIABILIDAD_MEDIA);
    }

    @Test
    void unaEmpresaConPedidoIdenticoElMismoDiaSeRetiene() {
        PedidoEvaluable nuevo = pedido(
            "Reposición de mercadería",
            "12.50",
            "18500.00",
            "Av. Santa Fe",
            2450,
            "2 A",
            "CABA"
        );
        PedidoEvaluable existente = new PedidoEvaluable(
            "REPOSICIÓN DE MERCADERÍA",
            new BigDecimal("12.5"),
            new BigDecimal("18500"),
            "AV. SANTA FE",
            2450,
            "2º A",
            "caba",
            HOY.minusHours(1)
        );

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Empresa,
            nuevo,
            List.of(existente),
            FiabilidadGeocodificacion.ALTA
        ));

        assertThat(resultado.retenido()).isTrue();
        assertThat(resultado.motivos())
            .containsExactly(MotivoRetencion.POSIBLE_DUPLICADO_COMERCIAL);
        assertThat(resultado.alertas()).containsExactly(AlertaRetencion.DUPLICADO_EMPRESA);
    }

    @Test
    void unParticularConPedidoIdenticoNoSeRetienePorDuplicado() {
        PedidoEvaluable nuevo = pedido(
            "Ropa deportiva",
            "4.00",
            "8500.00",
            "Bv. Chacabuco",
            900,
            "2 A",
            "Córdoba"
        );
        PedidoEvaluable existente = new PedidoEvaluable(
            "Ropa deportiva",
            new BigDecimal("4.00"),
            new BigDecimal("8500.00"),
            "Bv. Chacabuco",
            900,
            "2 A",
            "Córdoba",
            HOY.minusHours(1)
        );

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Particular,
            nuevo,
            List.of(existente),
            FiabilidadGeocodificacion.ALTA
        ));

        assertThat(resultado.retenido()).isFalse();
        assertThat(resultado.motivos()).isEmpty();
    }

    @Test
    void unaEmpresaConPedidoDistintoNoSeRetiene() {
        PedidoEvaluable nuevo = pedido(
            "Reposición de mercadería",
            "12.50",
            "18500.00",
            "Av. Santa Fe",
            2450,
            "2 A",
            "CABA"
        );
        PedidoEvaluable existente = new PedidoEvaluable(
            "Otra mercadería",
            new BigDecimal("12.50"),
            new BigDecimal("18500.00"),
            "Av. Santa Fe",
            2450,
            "2 A",
            "CABA",
            HOY.minusHours(1)
        );

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Empresa,
            nuevo,
            List.of(existente),
            FiabilidadGeocodificacion.ALTA
        ));

        assertThat(resultado.retenido()).isFalse();
    }

    @Test
    void unPedidoDeOtroDiaNoCuentaComoDuplicado() {
        PedidoEvaluable nuevo = pedido(
            "Reposición de mercadería",
            "12.50",
            "18500.00",
            "Av. Santa Fe",
            2450,
            "2 A",
            "CABA"
        );
        PedidoEvaluable existente = new PedidoEvaluable(
            "Reposición de mercadería",
            new BigDecimal("12.50"),
            new BigDecimal("18500.00"),
            "Av. Santa Fe",
            2450,
            "2 A",
            "CABA",
            HOY.minusDays(1)
        );

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Empresa,
            nuevo,
            List.of(existente),
            FiabilidadGeocodificacion.ALTA
        ));

        assertThat(resultado.retenido()).isFalse();
    }

    @Test
    void unPedidoDeEmpresaPuedeAcumularDireccionAmbiguaYDuplicado() {
        PedidoEvaluable nuevo = pedido(
            "Carga paletizada",
            "25.00",
            "25000.00",
            "S/N",
            0,
            null,
            "San Miguel"
        );
        PedidoEvaluable existente = new PedidoEvaluable(
            "Carga paletizada",
            new BigDecimal("25.00"),
            new BigDecimal("25000.00"),
            "S/N",
            0,
            null,
            "San Miguel",
            HOY.minusHours(1)
        );

        ResultadoRetencion resultado = motor.evaluar(evaluacion(
            TipoCliente.Empresa,
            nuevo,
            List.of(existente),
            FiabilidadGeocodificacion.ALTA
        ));

        assertThat(resultado.retenido()).isTrue();
        assertThat(resultado.motivos()).containsExactly(
            MotivoRetencion.DIRECCION_AMBIGUA,
            MotivoRetencion.POSIBLE_DUPLICADO_COMERCIAL
        );
        assertThat(resultado.alertas()).contains(
            AlertaRetencion.ALTURA_CERO,
            AlertaRetencion.SIN_NUMERO,
            AlertaRetencion.DUPLICADO_EMPRESA
        );
    }

    private EvaluacionRetencion evaluacion(
        TipoCliente tipoCliente,
        PedidoEvaluable nuevo,
        List<PedidoEvaluable> candidatos,
        FiabilidadGeocodificacion fiabilidad
    ) {
        return new EvaluacionRetencion(tipoCliente, nuevo, candidatos, fiabilidad);
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
