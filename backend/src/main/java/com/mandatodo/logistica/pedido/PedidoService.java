package com.mandatodo.logistica.pedido;

import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.cliente.Cliente;
import com.mandatodo.logistica.cliente.ClienteRepository;
import com.mandatodo.logistica.cliente.ClienteService;
import com.mandatodo.logistica.cliente.dto.ClienteRequest;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.pedido.dto.CambiarEstadoRequest;
import com.mandatodo.logistica.pedido.dto.CambioEstadoResponse;
import com.mandatodo.logistica.pedido.dto.CrearPedidoRequest;
import com.mandatodo.logistica.pedido.dto.CrearPedidoResponse;
import com.mandatodo.logistica.pedido.dto.PedidoResponse;
import com.mandatodo.logistica.pedido.dto.PedidoResponseFactory;
import com.mandatodo.logistica.pedido.dto.ResumenPedidosResponse;
import com.mandatodo.logistica.revision.EvaluacionRetencion;
import com.mandatodo.logistica.revision.MotivoRetencion;
import com.mandatodo.logistica.revision.MotorRetencion;
import com.mandatodo.logistica.revision.PedidoEvaluable;
import com.mandatodo.logistica.revision.ResultadoRetencion;
import com.mandatodo.logistica.revision.Revision;
import com.mandatodo.logistica.revision.RevisionRepository;
import com.mandatodo.logistica.shared.error.RecursoNoEncontradoException;
import com.mandatodo.logistica.shared.error.ReglaNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final ClienteRepository clienteRepository;
    private final OperarioRepository operarioRepository;
    private final PedidoRepository pedidoRepository;
    private final RevisionRepository revisionRepository;
    private final MotorRetencion motorRetencion;
    private final EntregaFallidaService entregaFallidaService;
    private final AuditoriaService auditoriaService;
    private final PedidoResponseFactory responseFactory;
    private final TarifaService tarifaService;
    private final Clock clock;

    public PedidoService(
        ClienteRepository clienteRepository,
        OperarioRepository operarioRepository,
        PedidoRepository pedidoRepository,
        RevisionRepository revisionRepository,
        MotorRetencion motorRetencion,
        EntregaFallidaService entregaFallidaService,
        AuditoriaService auditoriaService,
        PedidoResponseFactory responseFactory,
        TarifaService tarifaService,
        Clock clock
    ) {
        this.clienteRepository = clienteRepository;
        this.operarioRepository = operarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.revisionRepository = revisionRepository;
        this.motorRetencion = motorRetencion;
        this.entregaFallidaService = entregaFallidaService;
        this.auditoriaService = auditoriaService;
        this.responseFactory = responseFactory;
        this.tarifaService = tarifaService;
        this.clock = clock;
    }

    @Transactional
    public CrearPedidoResponse crear(CrearPedidoRequest request) {
        return crear(request, request.operarioId());
    }

    @Transactional
    public CrearPedidoResponse crear(CrearPedidoRequest request, Long operarioId) {
        Cliente cliente = resolverCliente(request);
        Operario operario = buscarOperario(operarioId);

        String descripcion = resolverDescripcion(request);
        BigDecimal montoEnvio = resolverMontoEnvio(request);
        Integer altura = request.altura() == null ? 0 : request.altura();
        LocalDate fechaProgramada = resolverFechaProgramada(request);
        FiabilidadGeocodificacion fiabilidad = resolverFiabilidad(request);

        LocalDate hoy = LocalDate.now(clock);
        LocalDateTime desde = hoy.atStartOfDay();
        LocalDateTime hasta = hoy.plusDays(1).atStartOfDay();
        List<Pedido> pedidosDelDia = pedidoRepository.findDelDia(cliente.getId(), desde, hasta);

        LocalDateTime fechaCreacion = LocalDateTime.now(clock);

        Pedido pedido = new Pedido(
            cliente,
            descripcion,
            request.pesoKg(),
            montoEnvio,
            request.calle().trim(),
            altura,
            normalizarOpcional(request.pisoDepto()),
            request.localidad().trim(),
            request.latitud(),
            request.longitud(),
            fechaProgramada,
            fechaCreacion
        );
        // La distancia se guarda para poder imprimirla en el remito: sin esto el PDF salía
        // con "Sin informar" aunque el operario la hubiera cargado al tarifar.
        pedido.setKilometros(request.kilometros());

        EvaluacionRetencion evaluacion = new EvaluacionRetencion(
            cliente.getTipoCliente(),
            PedidoEvaluable.desde(pedido),
            pedidosDelDia.stream()
                .filter(candidato -> candidato.getEstado() != EstadoPedido.CANCELADO_EN_TRANSITO)
                .map(PedidoEvaluable::desde)
                .toList(),
            fiabilidad
        );

        ResultadoRetencion resultado = motorRetencion.evaluar(evaluacion);
        pedido.setEstado(resultado.retenido() ? EstadoPedido.EN_REVISION : EstadoPedido.PENDIENTE);

        Pedido guardado = pedidoRepository.save(pedido);
        List<Revision> revisiones = new ArrayList<>();

        if (resultado.retenido()) {
            for (MotivoRetencion motivo : resultado.motivos()) {
                revisiones.add(revisionRepository.save(new Revision(guardado, motivo)));
            }
        }

        String actividad = "Alta de pedido #%d para %s"
            .formatted(guardado.getId(), cliente.getNombreRazonSocial());
        if (resultado.retenido()) {
            actividad += "; retenido por " + resultado.motivos().stream()
                .map(MotivoRetencion::getValor)
                .collect(Collectors.joining(" y "));
        }
        if (Boolean.TRUE.equals(request.pinAjustado())) {
            actividad += "; ubicación del pin corregida manualmente";
        }
        auditoriaService.registrar(operario, guardado, actividad, TipoEvento.ALTA);

        log.info(
            "Pedido {} creado en estado {}; alertas de retención: {}",
            guardado.getId(),
            guardado.getEstado(),
            resultado.alertas()
        );

        return new CrearPedidoResponse(responseFactory.crear(guardado, revisiones), resultado);
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        Pedido pedido = buscarPedido(id);
        return responseFactory.crear(pedido, revisionesDe(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(EstadoPedido estado) {
        List<Pedido> pedidos = estado == null
            ? pedidoRepository.findAllConCliente()
            : pedidoRepository.findByEstadoConCliente(estado);

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

    @Transactional(readOnly = true)
    public ResumenPedidosResponse resumen() {
        return new ResumenPedidosResponse(
            pedidoRepository.countByEstado(EstadoPedido.PENDIENTE),
            pedidoRepository.countByEstado(EstadoPedido.EN_REVISION),
            pedidoRepository.countByEstado(EstadoPedido.ENTREGADO),
            clienteRepository.count()
        );
    }

    @Transactional
    public CambioEstadoResponse cambiarEstado(Long id, CambiarEstadoRequest request) {
        return cambiarEstado(id, request, request.operarioId());
    }

    @Transactional
    public CambioEstadoResponse cambiarEstado(
        Long id,
        CambiarEstadoRequest request,
        Long operarioId
    ) {
        Pedido pedido = buscarPedido(id);
        Operario operario = buscarOperario(operarioId);

        EstadoPedido estadoAnterior = pedido.getEstado();
        EstadoPedido estadoNuevo = request.estado();

        if (estadoAnterior == estadoNuevo) {
            throw new ReglaNegocioException(
                "El pedido ya se encuentra en estado " + estadoNuevo
            );
        }
        if (estadoNuevo == EstadoPedido.EN_REVISION) {
            throw new ReglaNegocioException(
                "Para retener un pedido usá el flujo de revisión definido por POST /api/pedidos"
            );
        }
        if (estadoAnterior == EstadoPedido.EN_REVISION) {
            throw new ReglaNegocioException(
                "El pedido está en revisión; resolvelo con PUT /api/pedidos/{id}/revision"
            );
        }
        if (estadoAnterior == EstadoPedido.ENTREGADO
            || estadoAnterior == EstadoPedido.CANCELADO_EN_TRANSITO) {
            throw new ReglaNegocioException(
                "El pedido está en estado terminal " + estadoAnterior + " y no admite cambios"
            );
        }

        if (estadoNuevo == EstadoPedido.FALLIDO) {
            ResultadoReprogramacion resultado = entregaFallidaService.registrarFallo(
                pedido,
                request.motivoFalla(),
                request.notaFalla(),
                operario
            );
            return new CambioEstadoResponse(
                responseFactory.crear(pedido, revisionesDe(pedido)),
                true,
                resultado.recargoAplicado(),
                resultado.fechaReprogramada()
            );
        }

        if (estadoNuevo == EstadoPedido.CANCELADO_EN_TRANSITO) {
            if (!StringUtils.hasText(request.motivoCancelacion())) {
                throw new ReglaNegocioException(
                    "El motivo de cancelación es obligatorio para cancelar un pedido en tránsito"
                );
            }
            pedido.setMotivoCancelacion(request.motivoCancelacion().trim());
            pedido.setMotivoFalla(null);
        } else {
            pedido.setMotivoCancelacion(null);
            pedido.setMotivoFalla(null);
        }

        pedido.setEstado(estadoNuevo);

        String actividad = "Pedido #%d pasó de %s a %s"
            .formatted(pedido.getId(), estadoAnterior, estadoNuevo);
        if (estadoNuevo == EstadoPedido.CANCELADO_EN_TRANSITO) {
            actividad += " (motivo: " + request.motivoCancelacion().trim() + ")";
        }
        auditoriaService.registrar(operario, pedido, actividad, TipoEvento.CAMBIO_DE_ESTADO);

        return new CambioEstadoResponse(
            responseFactory.crear(pedido, revisionesDe(pedido)),
            false,
            null,
            null
        );
    }

    private Cliente resolverCliente(CrearPedidoRequest request) {
        if (request.clienteId() != null) {
            return clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No se encontró el cliente " + request.clienteId()
                ));
        }

        ClienteRequest clienteRequest = request.cliente();
        if (clienteRequest == null) {
            throw new ReglaNegocioException(
                "El cliente es obligatorio: enviá clienteId o los datos del cliente."
            );
        }

        String documento = ClienteService.normalizarDocumento(clienteRequest.numeroDocumento());
        if (!StringUtils.hasText(documento)) {
            throw new ReglaNegocioException("El número de documento del cliente es obligatorio.");
        }

        return clienteRepository.findByDocumentoNormalizado(documento)
            .orElseGet(() -> clienteRepository.save(new Cliente(
                normalizarTipoDocumento(clienteRequest.tipoDocumento()),
                documento,
                clienteRequest.nombreRazonSocial().trim(),
                StringUtils.hasText(clienteRequest.telefono())
                    ? clienteRequest.telefono().trim()
                    : "",
                clienteRequest.email().trim().toLowerCase(Locale.ROOT),
                clienteRequest.tipoCliente(),
                Boolean.TRUE.equals(clienteRequest.esFrecuente())
            )));
    }

    private String resolverDescripcion(CrearPedidoRequest request) {
        String descripcion = StringUtils.hasText(request.descripcion())
            ? request.descripcion()
            : request.descripcionPaquete();
        if (!StringUtils.hasText(descripcion)) {
            throw new ReglaNegocioException("La descripción del paquete es obligatoria.");
        }
        return descripcion.trim();
    }

    private BigDecimal resolverMontoEnvio(CrearPedidoRequest request) {
        BigDecimal kilometros = request.kilometros() == null
            ? BigDecimal.ZERO
            : request.kilometros();

        if (request.montoEnvio() != null) {
            // La capacidad máxima se valida incluso si el cliente envía un monto explícito.
            tarifaService.validarCapacidad(request.pesoKg(), kilometros);
            return request.montoEnvio().setScale(2, RoundingMode.HALF_UP);
        }

        return tarifaService.calcular(request.pesoKg(), kilometros);
    }

    private LocalDate resolverFechaProgramada(CrearPedidoRequest request) {
        if (request.fechaProgramada() != null) {
            return request.fechaProgramada();
        }
        if (request.fechaProgramadaEntrega() != null) {
            return request.fechaProgramadaEntrega();
        }
        return LocalDate.now(clock);
    }

    private FiabilidadGeocodificacion resolverFiabilidad(CrearPedidoRequest request) {
        if (request.fiabilidadGeocodificacion() != null) {
            return request.fiabilidadGeocodificacion();
        }
        return Boolean.TRUE.equals(request.esAmbigua())
            ? FiabilidadGeocodificacion.BAJA
            : null;
    }

    private String normalizarTipoDocumento(String tipoDocumento) {
        if (!StringUtils.hasText(tipoDocumento)) {
            throw new ReglaNegocioException("El tipo de documento del cliente es obligatorio.");
        }
        String normalizado = tipoDocumento.trim().toUpperCase(Locale.ROOT);
        if (!normalizado.equals("DNI") && !normalizado.equals("CUIT")) {
            throw new ReglaNegocioException("El tipo de documento debe ser DNI o CUIT.");
        }
        return normalizado;
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findConClientePorId(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el pedido " + id));
    }

    private Operario buscarOperario(Long id) {
        if (id == null) {
            throw new ReglaNegocioException(
                "No se pudo identificar al operario que realiza la operación."
            );
        }
        return operarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No se encontró el operario " + id
            ));
    }

    private List<Revision> revisionesDe(Pedido pedido) {
        return revisionRepository.findByPedidoIdOrderByIdAsc(pedido.getId());
    }

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
