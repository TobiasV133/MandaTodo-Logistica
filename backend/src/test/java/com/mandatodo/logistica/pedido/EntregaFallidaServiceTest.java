package com.mandatodo.logistica.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.cliente.Cliente;
import com.mandatodo.logistica.cliente.TipoCliente;
import com.mandatodo.logistica.operario.EstadoOperario;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.RolOperario;
import com.mandatodo.logistica.shared.calendario.CalendarioHabil;
import com.mandatodo.logistica.shared.error.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EntregaFallidaServiceTest {

    private static final Clock RELOJ = Clock.fixed(
        Instant.parse("2026-10-23T21:00:00Z"),
        ZoneId.of("America/Argentina/Buenos_Aires")
    );

    private AuditoriaService auditoriaService;
    private EntregaFallidaService service;
    private Operario operario;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        auditoriaService = mock(AuditoriaService.class);
        service = new EntregaFallidaService(
            RELOJ,
            new CalendarioHabil(Set.of()),
            auditoriaService
        );
        cliente = new Cliente(
            "DNI",
            "28456789",
            "Juan Carlos González",
            "11 3345-6789",
            "juan@example.com",
            TipoCliente.Particular,
            true
        );
        ReflectionTestUtils.setField(cliente, "id", 3L);
        operario = new Operario(
            "Laura Gómez",
            "laura@mandatodo.com",
            "hash",
            RolOperario.Admin,
            EstadoOperario.Activo
        );
        ReflectionTestUtils.setField(operario, "id", 1L);
    }

    @Test
    void aplicaRecargoDelCincuentaPorCientoYReprogramaAlSiguienteDiaHabil() {
        Pedido pedido = pedido(new BigDecimal("10000.00"), LocalDate.of(2026, 10, 23));
        pedido.setEstado(EstadoPedido.EN_TRANSITO);

        ResultadoReprogramacion resultado = service.registrarFallo(
            pedido,
            MotivoFalla.AUSENTE,
            "Timbre roto",
            operario
        );

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.FALLIDO);
        assertThat(pedido.getMotivoFalla()).isEqualTo(MotivoFalla.AUSENTE);
        assertThat(pedido.getMontoEnvio()).isEqualByComparingTo("15000.00");
        assertThat(pedido.getFechaProgramada()).isEqualTo(LocalDate.of(2026, 10, 26));

        assertThat(resultado.montoAnterior()).isEqualByComparingTo("10000.00");
        assertThat(resultado.recargoAplicado()).isEqualByComparingTo("5000.00");
        assertThat(resultado.montoFinal()).isEqualByComparingTo("15000.00");
        assertThat(resultado.fechaReprogramada()).isEqualTo(LocalDate.of(2026, 10, 26));

        verify(auditoriaService).registrar(
            eq(operario),
            eq(pedido),
            contains("recargo del 50%"),
            eq(TipoEvento.CAMBIO_DE_ESTADO)
        );
        verify(auditoriaService).registrar(
            eq(operario),
            eq(pedido),
            contains("Timbre roto"),
            eq(TipoEvento.CAMBIO_DE_ESTADO)
        );
    }

    @Test
    void aplicaRecargoSobreElMontoVigente() {
        Pedido pedido = pedido(new BigDecimal("15000.00"), LocalDate.of(2026, 10, 23));
        pedido.setEstado(EstadoPedido.EN_TRANSITO);

        ResultadoReprogramacion resultado = service.registrarFallo(
            pedido,
            MotivoFalla.DIRECCION_INCOMPLETA,
            null,
            operario
        );

        assertThat(resultado.montoAnterior()).isEqualByComparingTo("15000.00");
        assertThat(resultado.recargoAplicado()).isEqualByComparingTo("7500.00");
        assertThat(resultado.montoFinal()).isEqualByComparingTo("22500.00");
    }

    @Test
    void noReprogramaAntesDeLaFechaProgramadaOriginal() {
        Pedido pedido = pedido(new BigDecimal("10000.00"), LocalDate.of(2026, 10, 24));
        pedido.setEstado(EstadoPedido.PENDIENTE);

        ResultadoReprogramacion resultado = service.registrarFallo(
            pedido,
            MotivoFalla.AUSENTE,
            null,
            operario
        );

        assertThat(resultado.fechaReprogramada()).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void salteaUnFeriadoAdemasDelFinDeSemana() {
        EntregaFallidaService conFeriado = new EntregaFallidaService(
            RELOJ,
            new CalendarioHabil(Set.of(LocalDate.of(2026, 10, 26))),
            auditoriaService
        );
        Pedido pedido = pedido(new BigDecimal("10000.00"), LocalDate.of(2026, 10, 23));
        pedido.setEstado(EstadoPedido.EN_TRANSITO);

        ResultadoReprogramacion resultado = conFeriado.registrarFallo(
            pedido,
            MotivoFalla.AUSENTE,
            null,
            operario
        );

        assertThat(resultado.fechaReprogramada()).isEqualTo(LocalDate.of(2026, 10, 27));
    }

    @Test
    void exigeUnMotivoDeFalla() {
        Pedido pedido = pedido(new BigDecimal("10000.00"), LocalDate.of(2026, 10, 23));
        pedido.setEstado(EstadoPedido.EN_TRANSITO);

        assertThatThrownBy(() -> service.registrarFallo(pedido, null, null, operario))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("motivo de falla");
    }

    @Test
    void noVuelveAAplicarElRecargoSiElPedidoYaEstaFallido() {
        Pedido pedido = pedido(new BigDecimal("10000.00"), LocalDate.of(2026, 10, 23));
        pedido.setEstado(EstadoPedido.FALLIDO);

        assertThatThrownBy(() -> service.registrarFallo(
            pedido,
            MotivoFalla.AUSENTE,
            null,
            operario
        ))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("ya está marcado como FALLIDO");
    }

    @Test
    void noMarcaComoFallidoUnPedidoEntregado() {
        Pedido pedido = pedido(new BigDecimal("10000.00"), LocalDate.of(2026, 10, 23));
        pedido.setEstado(EstadoPedido.ENTREGADO);

        assertThatThrownBy(() -> service.registrarFallo(
            pedido,
            MotivoFalla.AUSENTE,
            null,
            operario
        ))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("ENTREGADO");
    }

    private Pedido pedido(BigDecimal montoEnvio, LocalDate fechaProgramada) {
        Pedido pedido = new Pedido(
            cliente,
            "Caja de electrónica",
            new BigDecimal("10.00"),
            montoEnvio,
            "Av. Corrientes",
            1234,
            "4 B",
            "CABA",
            new BigDecimal("-34.6037000"),
            new BigDecimal("-58.3816000"),
            fechaProgramada,
            LocalDateTime.of(2026, 10, 23, 10, 0)
        );
        ReflectionTestUtils.setField(pedido, "id", 9L);
        return pedido;
    }
}
