package com.mandatodo.logistica.revision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.cliente.Cliente;
import com.mandatodo.logistica.cliente.TipoCliente;
import com.mandatodo.logistica.operario.EstadoOperario;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.operario.RolOperario;
import com.mandatodo.logistica.pedido.EntregaFallidaService;
import com.mandatodo.logistica.pedido.EstadoPedido;
import com.mandatodo.logistica.pedido.MotivoFalla;
import com.mandatodo.logistica.pedido.Pedido;
import com.mandatodo.logistica.pedido.PedidoRepository;
import com.mandatodo.logistica.pedido.dto.PedidoResponse;
import com.mandatodo.logistica.pedido.dto.PedidoResponseFactory;
import com.mandatodo.logistica.revision.dto.ResolverRevisionRequest;
import com.mandatodo.logistica.shared.calendario.CalendarioHabil;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RevisionServiceTest {

    private static final Clock RELOJ = Clock.fixed(
        Instant.parse("2026-10-23T21:00:00Z"),
        ZoneId.of("America/Argentina/Buenos_Aires")
    );

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private RevisionRepository revisionRepository;

    @Mock
    private OperarioRepository operarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private PedidoResponseFactory responseFactory;

    private RevisionService revisionService;
    private Pedido pedido;
    private Revision revision;
    private Operario operario;

    @BeforeEach
    void setUp() {
        EntregaFallidaService entregaFallidaService = new EntregaFallidaService(
            RELOJ,
            new CalendarioHabil(Set.of()),
            auditoriaService
        );
        revisionService = new RevisionService(
            pedidoRepository,
            revisionRepository,
            operarioRepository,
            entregaFallidaService,
            auditoriaService,
            responseFactory
        );

        Cliente cliente = new Cliente(
            "CUIT",
            "30-71234567-8",
            "Distribuidora del Sur S.A.",
            "11 4567-8901",
            "contacto@delsur.com.ar",
            TipoCliente.Empresa,
            true
        );
        ReflectionTestUtils.setField(cliente, "id", 1L);

        pedido = new Pedido(
            cliente,
            "Carga paletizada",
            new BigDecimal("25.00"),
            new BigDecimal("10000.00"),
            "S/N",
            0,
            null,
            "San Miguel",
            null,
            null,
            LocalDate.of(2026, 10, 23),
            LocalDateTime.of(2026, 10, 23, 8, 5)
        );
        ReflectionTestUtils.setField(pedido, "id", 7L);
        pedido.setEstado(EstadoPedido.EN_REVISION);

        revision = new Revision(pedido, MotivoRetencion.DIRECCION_AMBIGUA);
        ReflectionTestUtils.setField(revision, "id", 3L);

        operario = new Operario(
            "Laura Gómez",
            "laura@mandatodo.com",
            "hash",
            RolOperario.Admin,
            EstadoOperario.Activo
        );
        ReflectionTestUtils.setField(operario, "id", 1L);

        when(pedidoRepository.findConClientePorId(7L)).thenReturn(Optional.of(pedido));
        when(revisionRepository.findByPedidoIdAndEstadoResolucionOrderByIdAsc(
            7L,
            EstadoResolucion.PENDIENTE
        )).thenReturn(List.of(revision));
        when(operarioRepository.findById(1L)).thenReturn(Optional.of(operario));
        when(revisionRepository.findByPedidoIdOrderByIdAsc(7L)).thenReturn(List.of(revision));
        when(responseFactory.crear(eq(pedido), anyList()))
            .thenReturn(mock(PedidoResponse.class));
    }

    @Test
    void aprobarRevisionPasaElPedidoAPendienteYPersisteLaCorreccion() {
        ResolverRevisionRequest request = new ResolverRevisionRequest(
            AccionRevision.APROBAR,
            1L,
            null,
            null,
            "Av. Corrientes",
            1234,
            null,
            "CABA",
            null,
            null
        );

        revisionService.resolver(7L, request);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(pedido.getCalle()).isEqualTo("Av. Corrientes");
        assertThat(pedido.getAltura()).isEqualTo(1234);
        assertThat(pedido.getLocalidad()).isEqualTo("CABA");
        assertThat(revision.getEstadoResolucion()).isEqualTo(EstadoResolucion.APROBADO);

        verify(revisionRepository).saveAll(List.of(revision));
        verify(auditoriaService).registrar(
            eq(operario),
            eq(pedido),
            contains("Revisión APROBADA"),
            eq(TipoEvento.EDICION)
        );
    }

    @Test
    void rechazarRevisionAplicaLaLogicaDeEntregaFallida() {
        ResolverRevisionRequest request = new ResolverRevisionRequest(
            AccionRevision.RECHAZAR,
            1L,
            MotivoFalla.DIRECCION_INEXISTENTE,
            "Dirección validada como inexistente",
            null,
            null,
            null,
            null,
            null,
            null
        );

        revisionService.resolver(7L, request);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.FALLIDO);
        assertThat(pedido.getMotivoFalla()).isEqualTo(MotivoFalla.DIRECCION_INEXISTENTE);
        assertThat(pedido.getMontoEnvio()).isEqualByComparingTo("15000.00");
        assertThat(pedido.getFechaProgramada()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(revision.getEstadoResolucion()).isEqualTo(EstadoResolucion.RECHAZADO);

        verify(auditoriaService).registrar(
            eq(operario),
            eq(pedido),
            contains("Revisión RECHAZADA"),
            eq(TipoEvento.EDICION)
        );
        verify(auditoriaService).registrar(
            eq(operario),
            eq(pedido),
            contains("reprogramado para 2026-10-26"),
            eq(TipoEvento.CAMBIO_DE_ESTADO)
        );
    }
}
