package com.mandatodo.logistica.revision;

import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.pedido.EntregaFallidaService;
import com.mandatodo.logistica.pedido.EstadoPedido;
import com.mandatodo.logistica.pedido.MotivoFalla;
import com.mandatodo.logistica.pedido.Pedido;
import com.mandatodo.logistica.pedido.PedidoRepository;
import com.mandatodo.logistica.pedido.dto.PedidoResponse;
import com.mandatodo.logistica.pedido.dto.PedidoResponseFactory;
import com.mandatodo.logistica.revision.dto.ResolverRevisionRequest;
import com.mandatodo.logistica.shared.error.RecursoNoEncontradoException;
import com.mandatodo.logistica.shared.error.ReglaNegocioException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RevisionService {

    private final PedidoRepository pedidoRepository;
    private final RevisionRepository revisionRepository;
    private final OperarioRepository operarioRepository;
    private final EntregaFallidaService entregaFallidaService;
    private final AuditoriaService auditoriaService;
    private final PedidoResponseFactory responseFactory;

    public RevisionService(
        PedidoRepository pedidoRepository,
        RevisionRepository revisionRepository,
        OperarioRepository operarioRepository,
        EntregaFallidaService entregaFallidaService,
        AuditoriaService auditoriaService,
        PedidoResponseFactory responseFactory
    ) {
        this.pedidoRepository = pedidoRepository;
        this.revisionRepository = revisionRepository;
        this.operarioRepository = operarioRepository;
        this.entregaFallidaService = entregaFallidaService;
        this.auditoriaService = auditoriaService;
        this.responseFactory = responseFactory;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarEnRevision() {
        List<Pedido> pedidos = pedidoRepository.findByEstadoConCliente(EstadoPedido.EN_REVISION);
        if (pedidos.isEmpty()) {
            return List.of();
        }

        List<Long> ids = pedidos.stream().map(Pedido::getId).toList();
        Map<Long, List<Revision>> revisionesPorPedido = revisionRepository
            .findByPedidoIdInOrderByIdAsc(ids)
            .stream()
            .collect(Collectors.groupingBy(
                revision -> revision.getPedido().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        return pedidos.stream()
            .map(pedido -> responseFactory.crear(
                pedido,
                revisionesPorPedido.getOrDefault(pedido.getId(), List.of())
            ))
            .toList();
    }

    @Transactional
    public PedidoResponse resolver(Long idPedido, ResolverRevisionRequest request) {
        Pedido pedido = pedidoRepository.findConClientePorId(idPedido)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No se encontró el pedido " + idPedido
            ));

        if (pedido.getEstado() != EstadoPedido.EN_REVISION) {
            throw new ReglaNegocioException("El pedido no está en estado EN_REVISION");
        }

        List<Revision> pendientes = revisionRepository
            .findByPedidoIdAndEstadoResolucionOrderByIdAsc(
                idPedido,
                EstadoResolucion.PENDIENTE
            );
        if (pendientes.isEmpty()) {
            throw new ReglaNegocioException("El pedido no tiene revisiones pendientes");
        }

        Operario operario = operarioRepository.findById(request.operarioId())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No se encontró el operario " + request.operarioId()
            ));

        EstadoResolucion resolucion = request.accion() == AccionRevision.APROBAR
            ? EstadoResolucion.APROBADO
            : EstadoResolucion.RECHAZADO;

        pendientes.forEach(revision -> revision.setEstadoResolucion(resolucion));
        revisionRepository.saveAll(pendientes);

        if (request.accion() == AccionRevision.APROBAR) {
            aplicarCorreccion(pedido, request);
            pedido.setEstado(EstadoPedido.PENDIENTE);
            pedido.setMotivoFalla(null);
            pedido.setMotivoCancelacion(null);
            auditoriaService.registrar(
                operario,
                pedido,
                "Revisión APROBADA para el pedido #" + pedido.getId()
                    + "; el pedido pasó a PENDIENTE",
                TipoEvento.EDICION
            );
        } else {
            MotivoFalla motivo = request.motivoFalla() == null
                ? MotivoFalla.DIRECCION_INEXISTENTE
                : request.motivoFalla();
            auditoriaService.registrar(
                operario,
                pedido,
                "Revisión RECHAZADA para el pedido #" + pedido.getId(),
                TipoEvento.EDICION
            );
            entregaFallidaService.registrarFallo(
                pedido,
                motivo,
                request.notaFalla(),
                operario
            );
        }

        return responseFactory.crear(
            pedido,
            revisionRepository.findByPedidoIdOrderByIdAsc(pedido.getId())
        );
    }

    private void aplicarCorreccion(Pedido pedido, ResolverRevisionRequest request) {
        if (StringUtils.hasText(request.calle())) {
            pedido.setCalle(request.calle().trim());
        }
        if (request.altura() != null) {
            pedido.setAltura(request.altura());
        }
        if (request.pisoDepto() != null) {
            pedido.setPisoDepto(
                StringUtils.hasText(request.pisoDepto())
                    ? request.pisoDepto().trim()
                    : null
            );
        }
        if (StringUtils.hasText(request.localidad())) {
            pedido.setLocalidad(request.localidad().trim());
        }
        if (request.latitud() != null) {
            pedido.setLatitud(request.latitud());
        }
        if (request.longitud() != null) {
            pedido.setLongitud(request.longitud());
        }
    }
}
