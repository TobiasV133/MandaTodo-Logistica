package com.mandatodo.logistica.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.cliente.Cliente;
import com.mandatodo.logistica.cliente.ClienteRepository;
import com.mandatodo.logistica.cliente.TipoCliente;
import com.mandatodo.logistica.operario.EstadoOperario;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.operario.RolOperario;
import com.mandatodo.logistica.pedido.dto.CrearPedidoRequest;
import com.mandatodo.logistica.pedido.dto.CrearPedidoResponse;
import com.mandatodo.logistica.pedido.dto.PedidoResponse;
import com.mandatodo.logistica.pedido.dto.PedidoResponseFactory;
import com.mandatodo.logistica.revision.MotivoRetencion;
import com.mandatodo.logistica.revision.MotorRetencion;
import com.mandatodo.logistica.revision.Revision;
import com.mandatodo.logistica.revision.RevisionRepository;
import com.mandatodo.logistica.shared.calendario.CalendarioHabil;
import com.mandatodo.logistica.shared.error.RecursoNoEncontradoException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final Clock RELOJ = Clock.fixed(
        Instant.parse("2026-10-23T12:00:00Z"),
        ZoneId.of("America/Argentina/Buenos_Aires")
    );

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private OperarioRepository operarioRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private RevisionRepository revisionRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private PedidoResponseFactory responseFactory;

    private PedidoService pedidoService;
    private Cliente empresa;
    private Cliente particular;
    private Operario operario;

    @BeforeEach
    void setUp() {
        EntregaFallidaService entregaFallidaService = new EntregaFallidaService(
            RELOJ,
            new CalendarioHabil(Set.of()),
            auditoriaService
        );
        pedidoService = new PedidoService(
            clienteRepository,
            operarioRepository,
            pedidoRepository,
            revisionRepository,
            new MotorRetencion(),
            entregaFallidaService,
            auditoriaService,
            responseFactory,
            RELOJ
        );

        empresa = cliente(1L, TipoCliente.Empresa, "Distribuidora del Sur S.A.");
        particular = cliente(2L, TipoCliente.Particular, "Juan Carlos González");
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
    void creaPedidoEnRevisionCuandoLaAlturaEsCero() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(operarioRepository.findById(1L)).thenReturn(Optional.of(operario));
        when(pedidoRepository.findDelDia(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> {
            Pedido pedido = invocacion.getArgument(0);
            ReflectionTestUtils.setField(pedido, "id", 10L);
            return pedido;
        });
        when(revisionRepository.save(any(Revision.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(responseFactory.crear(any(Pedido.class), anyList()))
            .thenReturn(mock(PedidoResponse.class));

        CrearPedidoResponse response = pedidoService.crear(
            request(1L, "Av. Corrientes", 0, "CABA")
        );

        assertThat(response.retencion().retenido()).isTrue();
        assertThat(response.retencion().motivos())
            .containsExactly(MotivoRetencion.DIRECCION_AMBIGUA);

        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(pedidoCaptor.capture());
        assertThat(pedidoCaptor.getValue().getEstado()).isEqualTo(EstadoPedido.EN_REVISION);

        ArgumentCaptor<Revision> revisionCaptor = ArgumentCaptor.forClass(Revision.class);
        verify(revisionRepository).save(revisionCaptor.capture());
        assertThat(revisionCaptor.getValue().getMotivoRetencion())
            .isEqualTo(MotivoRetencion.DIRECCION_AMBIGUA);
        assertThat(revisionCaptor.getValue().getEstadoResolucion())
            .isEqualTo(com.mandatodo.logistica.revision.EstadoResolucion.PENDIENTE);

        verify(auditoriaService).registrar(
            eq(operario),
            any(Pedido.class),
            contains("retenido por Dirección ambigua"),
            eq(TipoEvento.ALTA)
        );
    }

    @Test
    void creaPedidoPendienteCuandoLaDireccionEsCompletaYEsParticular() {
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(particular));
        when(operarioRepository.findById(1L)).thenReturn(Optional.of(operario));
        when(pedidoRepository.findDelDia(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> {
            Pedido pedido = invocacion.getArgument(0);
            ReflectionTestUtils.setField(pedido, "id", 11L);
            return pedido;
        });
        when(responseFactory.crear(any(Pedido.class), anyList()))
            .thenReturn(mock(PedidoResponse.class));

        CrearPedidoResponse response = pedidoService.crear(
            request(2L, "Bv. Chacabuco", 900, "Córdoba")
        );

        assertThat(response.retencion().retenido()).isFalse();

        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(pedidoCaptor.capture());
        assertThat(pedidoCaptor.getValue().getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        verify(revisionRepository, never()).save(any(Revision.class));

        verify(auditoriaService).registrar(
            eq(operario),
            any(Pedido.class),
            contains("Alta de pedido #11"),
            eq(TipoEvento.ALTA)
        );
    }

    @Test
    void unaEmpresaConPedidoIdenticoElMismoDiaQuedaEnRevision() {
        Pedido existente = pedidoExistente(empresa);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(operarioRepository.findById(1L)).thenReturn(Optional.of(operario));
        when(pedidoRepository.findDelDia(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(existente));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> {
            Pedido pedido = invocacion.getArgument(0);
            ReflectionTestUtils.setField(pedido, "id", 12L);
            return pedido;
        });
        when(revisionRepository.save(any(Revision.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(responseFactory.crear(any(Pedido.class), anyList()))
            .thenReturn(mock(PedidoResponse.class));

        CrearPedidoResponse response = pedidoService.crear(
            request(1L, "Av. Corrientes", 1234, "CABA")
        );

        assertThat(response.retencion().retenido()).isTrue();
        assertThat(response.retencion().motivos())
            .containsExactly(MotivoRetencion.POSIBLE_DUPLICADO_COMERCIAL);

        ArgumentCaptor<Revision> revisionCaptor = ArgumentCaptor.forClass(Revision.class);
        verify(revisionRepository).save(revisionCaptor.capture());
        assertThat(revisionCaptor.getValue().getMotivoRetencion())
            .isEqualTo(MotivoRetencion.POSIBLE_DUPLICADO_COMERCIAL);
    }

    @Test
    void unPedidoCanceladoNoBloqueaUnNuevoPedidoIdentico() {
        Pedido existente = pedidoExistente(empresa);
        existente.setEstado(EstadoPedido.CANCELADO_EN_TRANSITO);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(operarioRepository.findById(1L)).thenReturn(Optional.of(operario));
        when(pedidoRepository.findDelDia(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(existente));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> {
            Pedido pedido = invocacion.getArgument(0);
            ReflectionTestUtils.setField(pedido, "id", 13L);
            return pedido;
        });
        when(responseFactory.crear(any(Pedido.class), anyList()))
            .thenReturn(mock(PedidoResponse.class));

        CrearPedidoResponse response = pedidoService.crear(
            request(1L, "Av. Corrientes", 1234, "CABA")
        );

        assertThat(response.retencion().retenido()).isFalse();
        verify(revisionRepository, never()).save(any(Revision.class));
    }

    @Test
    void fallaSiElClienteNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.crear(
            request(99L, "Av. Corrientes", 1234, "CABA")
        ))
            .isInstanceOf(RecursoNoEncontradoException.class)
            .hasMessageContaining("cliente 99");
    }

    private CrearPedidoRequest request(Long clienteId, String calle, int altura, String localidad) {
        return new CrearPedidoRequest(
            operario.getId(),
            clienteId,
            "Caja de electrónica",
            new BigDecimal("10.00"),
            new BigDecimal("5000.00"),
            calle,
            altura,
            "4 B",
            localidad,
            null,
            null,
            LocalDate.of(2026, 10, 23),
            FiabilidadGeocodificacion.ALTA
        );
    }

    private Pedido pedidoExistente(Cliente cliente) {
        Pedido pedido = new Pedido(
            cliente,
            "Caja de electrónica",
            new BigDecimal("10.00"),
            new BigDecimal("5000.00"),
            "Av. Corrientes",
            1234,
            "4 B",
            "CABA",
            null,
            null,
            LocalDate.of(2026, 10, 23),
            LocalDateTime.of(2026, 10, 23, 9, 0)
        );
        ReflectionTestUtils.setField(pedido, "id", 5L);
        return pedido;
    }

    private Cliente cliente(Long id, TipoCliente tipoCliente, String nombre) {
        Cliente cliente = new Cliente(
            tipoCliente == TipoCliente.Empresa ? "CUIT" : "DNI",
            tipoCliente == TipoCliente.Empresa ? "30-71234567-8" : "28456789",
            nombre,
            "11 4567-8901",
            "contacto@example.com",
            tipoCliente,
            false
        );
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }
}
