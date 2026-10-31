package com.mandatodo.logistica.remito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mandatodo.logistica.auditoria.AuditoriaRepository;
import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.cliente.Cliente;
import com.mandatodo.logistica.cliente.TipoCliente;
import com.mandatodo.logistica.operario.EstadoOperario;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.operario.RolOperario;
import com.mandatodo.logistica.pedido.EstadoPedido;
import com.mandatodo.logistica.pedido.Pedido;
import com.mandatodo.logistica.pedido.PedidoRepository;
import com.mandatodo.logistica.remito.dto.ImprimirRemitoRequest;
import com.mandatodo.logistica.shared.error.RecursoNoEncontradoException;
import com.mandatodo.logistica.shared.error.ReglaNegocioException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RemitoServiceTest {

    private static final Clock RELOJ = Clock.fixed(
        Instant.parse("2026-10-23T12:00:00Z"),
        ZoneId.of("America/Argentina/Buenos_Aires")
    );
    private static final byte[] PDF = "%PDF-1.4 remito".getBytes(StandardCharsets.ISO_8859_1);

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private OperarioRepository operarioRepository;

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private RemitoPdfGenerator generador;

    private RemitoService servicio;
    private Pedido pedido;
    private Operario operario;

    @BeforeEach
    void setUp() {
        servicio = new RemitoService(
            pedidoRepository,
            operarioRepository,
            auditoriaRepository,
            auditoriaService,
            generador,
            new EmpresaRemitoProperties(),
            RELOJ
        );

        Cliente cliente = new Cliente(
            "CUIT",
            "30-71234567-8",
            "Distribuidora del Sur S.A.",
            "11 4567-8901",
            "contacto@delsur.com.ar",
            TipoCliente.Empresa,
            false
        );
        ReflectionTestUtils.setField(cliente, "id", 1L);

        pedido = new Pedido(
            cliente,
            "Caja de electrónica",
            new BigDecimal("10.00"),
            new BigDecimal("10.00"),
            new BigDecimal("18500.00"),
            new BigDecimal("18500.00"),
            BigDecimal.ZERO,
            new BigDecimal("18500.00"),
            "Av. Corrientes",
            1234,
            "4 B",
            "Ciudad Autónoma de Buenos Aires",
            null,
            null,
            LocalDate.of(2026, 10, 26),
            LocalDateTime.of(2026, 10, 23, 8, 0)
        );
        ReflectionTestUtils.setField(pedido, "id", 42L);

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
    void generaElPrimerRemitoYLoRegistraComoAlta() {
        prepararPedidoYOperario();
        when(auditoriaRepository.countByPedido_IdAndActividadStartingWith(42L, "Remito "))
            .thenReturn(0L);
        permitirGeneracion();

        RemitoGenerado resultado = servicio.imprimir(42L, request(null, "45.00"));

        assertThat(resultado.reimpresion()).isFalse();
        assertThat(resultado.numero()).isEqualTo("R-00000042");
        assertThat(resultado.nombreArchivo()).isEqualTo("remito-R-00000042.pdf");
        assertThat(resultado.contenido()).isEqualTo(PDF);

        ArgumentCaptor<DatosRemito> captor = ArgumentCaptor.forClass(DatosRemito.class);
        verify(generador).generar(captor.capture());
        assertThat(captor.getValue().reimpresion()).isFalse();
        assertThat(captor.getValue().motivoReimpresion()).isNull();
        assertThat(captor.getValue().kilometros()).isEqualByComparingTo("45.00");
        assertThat(captor.getValue().direccion()).isEqualTo("Av. Corrientes 1234, 4 B");
        assertThat(captor.getValue().fechaEmision())
            .isEqualTo(LocalDateTime.of(2026, 10, 23, 9, 0));

        verify(auditoriaService).registrar(
            eq(operario),
            eq(pedido),
            contains("Remito original emitido para el pedido #42 (R-00000042)"),
            eq(TipoEvento.ALTA)
        );
    }

    @Test
    void imprimeLosKilometrosGuardadosEnElPedidoCuandoNoSeInformanAlImprimir() {
        prepararPedidoYOperario();
        pedido.setKilometros(new BigDecimal("12.50"));
        when(auditoriaRepository.countByPedido_IdAndActividadStartingWith(42L, "Remito "))
            .thenReturn(0L);
        permitirGeneracion();

        servicio.imprimir(42L, request(null, null));

        ArgumentCaptor<DatosRemito> captor = ArgumentCaptor.forClass(DatosRemito.class);
        verify(generador).generar(captor.capture());
        assertThat(captor.getValue().kilometros()).isEqualByComparingTo("12.50");
    }

    @Test
    void losKilometrosInformadosAlImprimirPisanLosDelPedido() {
        prepararPedidoYOperario();
        pedido.setKilometros(new BigDecimal("12.50"));
        when(auditoriaRepository.countByPedido_IdAndActividadStartingWith(42L, "Remito "))
            .thenReturn(0L);
        permitirGeneracion();

        servicio.imprimir(42L, request(null, "45.00"));

        ArgumentCaptor<DatosRemito> captor = ArgumentCaptor.forClass(DatosRemito.class);
        verify(generador).generar(captor.capture());
        assertThat(captor.getValue().kilometros()).isEqualByComparingTo("45.00");
    }

    @Test
    void separaElSubtotalYElRecargoDeUnaEntregaFallida() {
        prepararPedidoYOperario();
        pedido.setMontoEnvio(new BigDecimal("9000.00"));
        when(auditoriaRepository.countByPedido_IdAndActividadStartingWith(42L, "Remito "))
            .thenReturn(0L);
        when(auditoriaRepository.countByPedido_IdAndActividadContaining(
            42L,
            "marcado como FALLIDO"
        )).thenReturn(1L);
        permitirGeneracion();

        servicio.imprimir(42L, request(null, "45.00"));

        ArgumentCaptor<DatosRemito> captor = ArgumentCaptor.forClass(DatosRemito.class);
        verify(generador).generar(captor.capture());
        assertThat(captor.getValue().montoEnvio()).isEqualByComparingTo("9000.00");
        assertThat(captor.getValue().subtotal()).isEqualByComparingTo("6000.00");
        assertThat(captor.getValue().recargo()).isEqualByComparingTo("3000.00");
    }

    @Test
    void acumulaElRecargoSiElPedidoFalloMasDeUnaVez() {
        prepararPedidoYOperario();
        pedido.setMontoEnvio(new BigDecimal("13500.00"));
        when(auditoriaRepository.countByPedido_IdAndActividadStartingWith(42L, "Remito "))
            .thenReturn(0L);
        when(auditoriaRepository.countByPedido_IdAndActividadContaining(
            42L,
            "marcado como FALLIDO"
        )).thenReturn(2L);
        permitirGeneracion();

        servicio.imprimir(42L, request(null, "45.00"));

        ArgumentCaptor<DatosRemito> captor = ArgumentCaptor.forClass(DatosRemito.class);
        verify(generador).generar(captor.capture());
        assertThat(captor.getValue().subtotal()).isEqualByComparingTo("6000.00");
        assertThat(captor.getValue().recargo()).isEqualByComparingTo("7500.00");
    }

    @Test
    void ignoraElMotivoCuandoTodaviaNoExistiaUnRemito() {
        prepararPedidoYOperario();
        when(auditoriaRepository.countByPedido_IdAndActividadStartingWith(42L, "Remito "))
            .thenReturn(0L);
        permitirGeneracion();

        RemitoGenerado resultado = servicio.imprimir(42L, request("Extravío", null));

        assertThat(resultado.reimpresion()).isFalse();
        assertThat(resultado.motivoReimpresion()).isNull();

        ArgumentCaptor<DatosRemito> captor = ArgumentCaptor.forClass(DatosRemito.class);
        verify(generador).generar(captor.capture());
        assertThat(captor.getValue().motivoReimpresion()).isNull();
        assertThat(captor.getValue().kilometros()).isEqualByComparingTo("10.00");
    }

    @Test
    void exigeElMotivoCuandoElRemitoYaFueGenerado() {
        prepararPedidoYOperario();
        when(auditoriaRepository.countByPedido_IdAndActividadStartingWith(42L, "Remito "))
            .thenReturn(1L);

        assertThatThrownBy(() -> servicio.imprimir(42L, request("   ", null)))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("motivo de reimpresión es obligatorio")
            .hasMessageContaining("R-00000042");

        verifyNoInteractions(generador);
        verify(auditoriaService, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void registraLaReimpresionConSuMotivo() {
        prepararPedidoYOperario();
        when(auditoriaRepository.countByPedido_IdAndActividadStartingWith(42L, "Remito "))
            .thenReturn(1L);
        permitirGeneracion();

        RemitoGenerado resultado = servicio.imprimir(
            42L,
            request("  Extravío del remito original  ", "45.00")
        );

        assertThat(resultado.reimpresion()).isTrue();
        assertThat(resultado.motivoReimpresion()).isEqualTo("Extravío del remito original");

        ArgumentCaptor<DatosRemito> captor = ArgumentCaptor.forClass(DatosRemito.class);
        verify(generador).generar(captor.capture());
        assertThat(captor.getValue().reimpresion()).isTrue();
        assertThat(captor.getValue().motivoReimpresion())
            .isEqualTo("Extravío del remito original");

        verify(auditoriaService).registrar(
            eq(operario),
            eq(pedido),
            contains("Remito duplicado emitido para el pedido #42 (R-00000042); motivo: Extravío del remito original"),
            eq(TipoEvento.REIMPRESION)
        );
    }

    @Test
    void noGeneraRemitoDeUnPedidoEnRevision() {
        pedido.setEstado(EstadoPedido.EN_REVISION);
        prepararPedidoYOperario();

        assertThatThrownBy(() -> servicio.imprimir(42L, request(null, null)))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("en revisión");

        verifyNoInteractions(generador);
        verify(auditoriaService, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void noGeneraRemitoDeUnPedidoCanceladoEnTransito() {
        pedido.setEstado(EstadoPedido.CANCELADO_EN_TRANSITO);
        prepararPedidoYOperario();

        assertThatThrownBy(() -> servicio.imprimir(42L, request(null, null)))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("cancelado en tránsito");

        verifyNoInteractions(generador);
    }

    @Test
    void fallaSiElPedidoNoExiste() {
        when(pedidoRepository.findConClientePorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.imprimir(99L, request(null, null)))
            .isInstanceOf(RecursoNoEncontradoException.class)
            .hasMessageContaining("pedido 99");
    }

    @Test
    void fallaSiElOperarioNoExiste() {
        when(pedidoRepository.findConClientePorId(42L)).thenReturn(Optional.of(pedido));
        when(operarioRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.imprimir(42L, new ImprimirRemitoRequest(7L, null, null)))
            .isInstanceOf(RecursoNoEncontradoException.class)
            .hasMessageContaining("operario 7");
    }

    private void prepararPedidoYOperario() {
        when(pedidoRepository.findConClientePorId(42L)).thenReturn(Optional.of(pedido));
        when(operarioRepository.findById(1L)).thenReturn(Optional.of(operario));
    }

    private void permitirGeneracion() {
        when(generador.generar(any(DatosRemito.class))).thenReturn(PDF);
    }

    private ImprimirRemitoRequest request(String motivo, String kilometros) {
        return new ImprimirRemitoRequest(
            1L,
            motivo,
            kilometros == null ? null : new BigDecimal(kilometros)
        );
    }
}
