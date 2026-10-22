package com.mandatodo.backend.service;

import com.mandatodo.backend.dto.pedido.PedidoRequest;
import com.mandatodo.backend.dto.pedido.PedidoResponse;
import com.mandatodo.backend.dto.pedido.ResumenPedidosResponse;
import com.mandatodo.backend.exception.ApiException;
import com.mandatodo.backend.model.AuditoriaLog;
import com.mandatodo.backend.model.Cliente;
import com.mandatodo.backend.model.EstadoPedido;
import com.mandatodo.backend.model.Operario;
import com.mandatodo.backend.model.Pedido;
import com.mandatodo.backend.model.RevisionPedido;
import com.mandatodo.backend.model.TipoCliente;
import com.mandatodo.backend.repository.AuditoriaLogRepository;
import com.mandatodo.backend.repository.OperarioRepository;
import com.mandatodo.backend.repository.PedidoRepository;
import com.mandatodo.backend.repository.RevisionPedidoRepository;
import com.mandatodo.backend.util.Direcciones;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PedidoService {

    private static final DateTimeFormatter FORMATO_CODIGO = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_DETALLE_AUDITORIA = 2000;

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final TarifaService tarifaService;
    private final EstadoPedidoResolver estadoResolver;
    private final OperarioRepository operarioRepository;
    private final RevisionPedidoRepository revisionPedidoRepository;
    private final AuditoriaLogRepository auditoriaLogRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteService clienteService,
            TarifaService tarifaService,
            EstadoPedidoResolver estadoResolver,
            OperarioRepository operarioRepository,
            RevisionPedidoRepository revisionPedidoRepository,
            AuditoriaLogRepository auditoriaLogRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.clienteService = clienteService;
        this.tarifaService = tarifaService;
        this.estadoResolver = estadoResolver;
        this.operarioRepository = operarioRepository;
        this.revisionPedidoRepository = revisionPedidoRepository;
        this.auditoriaLogRepository = auditoriaLogRepository;
    }

    @Transactional
    public PedidoResponse crear(PedidoRequest request, Integer idOperario) {
        if (idOperario == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Iniciá sesión para registrar pedidos.");
        }

        Cliente cliente = clienteService.obtenerOCrear(request.cliente());

        boolean direccionIncompleta = Direcciones.esIncompleta(
                request.calle(),
                request.altura(),
                request.localidad()
        );
        boolean direccionAmbigua = Boolean.TRUE.equals(request.esAmbigua()) || direccionIncompleta;

        boolean duplicadoComercial = false;
        if (cliente.getTipoCliente() == TipoCliente.Empresa
                && !Boolean.TRUE.equals(cliente.getEsFrecuente())) {
            LocalDate hoy = LocalDate.now();
            duplicadoComercial = pedidoRepository.existsDuplicadoComercialDelDia(
                    cliente.getIdCliente(),
                    request.calle(),
                    request.altura(),
                    Direcciones.limpiarOpcional(request.pisoDepto()),
                    request.localidad(),
                    request.descripcionPaquete(),
                    hoy.atStartOfDay(),
                    hoy.plusDays(1).atStartOfDay(),
                    EstadoPedido.CANCELADO_EN_TRANSITO
            );
        }

        EstadoPedidoResolver.Resolucion resolucion = estadoResolver.resolver(
                cliente.getTipoCliente(),
                Boolean.TRUE.equals(cliente.getEsFrecuente()),
                direccionAmbigua,
                direccionIncompleta,
                duplicadoComercial
        );

        TarifaService.Tarifa tarifa = tarifaService.calcular(request.pesoKg(), request.kilometros());

        Pedido pedido = new Pedido();
        pedido.setCodigoPedido(generarCodigoUnico());
        pedido.setCliente(cliente);
        pedido.setIdPedidoOrigen(request.idPedidoOrigen());
        pedido.setDescripcionPaquete(request.descripcionPaquete().trim());
        pedido.setPesoKg(request.pesoKg());
        pedido.setKilometros(request.kilometros());
        pedido.setPrecioBase(tarifa.precioBase());
        pedido.setRecargo(tarifa.recargo());
        pedido.setPrecioTotal(tarifa.precioTotal());
        pedido.setCalle(request.calle().trim());
        pedido.setAltura(request.altura());
        pedido.setPisoDepto(Direcciones.limpiarOpcional(request.pisoDepto()));
        pedido.setLocalidad(request.localidad().trim());
        pedido.setLatitud(request.latitud());
        pedido.setLongitud(request.longitud());
        pedido.setEstado(resolucion.estado());
        pedido.setEsReintento(false);
        pedido.setFechaProgramadaEntrega(request.fechaProgramadaEntrega());
        pedido.setIdOperarioUltimaModificacion(idOperario);
        pedido.setFechaUltimaModificacion(LocalDateTime.now());

        Pedido guardado = pedidoRepository.save(pedido);
        registrarRetencion(guardado, resolucion.observacion());
        registrarAuditoriaAlta(guardado, idOperario, request.observacionGeo());

        String motivoRevision = guardado.getEstado() == EstadoPedido.EN_REVISION
                ? resolucion.observacion()
                : null;
        return aRespuesta(guardado, resolucion.observacion(), motivoRevision);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(EstadoPedido estado) {
        List<Pedido> pedidos = estado == null
                ? pedidoRepository.findAllByOrderByFechaCreacionDesc()
                : pedidoRepository.findByEstadoOrderByFechaCreacionDesc(estado);

        Map<Integer, String> motivosRevision = buscarMotivosRevision(pedidos);

        return pedidos.stream()
                .map(pedido -> aRespuesta(pedido, null, motivosRevision.get(pedido.getIdPedido())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Integer idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el pedido solicitado."));
        return aRespuesta(pedido, null, buscarMotivosRevision(List.of(pedido)).get(idPedido));
    }

    @Transactional(readOnly = true)
    public ResumenPedidosResponse resumen() {
        return new ResumenPedidosResponse(
                pedidoRepository.countByEstado(EstadoPedido.PENDIENTE),
                pedidoRepository.countByEstado(EstadoPedido.EN_REVISION),
                pedidoRepository.countByEstado(EstadoPedido.ENTREGADO),
                clienteService.contar()
        );
    }

    private void registrarRetencion(Pedido pedido, String motivo) {
        if (pedido.getEstado() != EstadoPedido.EN_REVISION) {
            return;
        }

        RevisionPedido revision = new RevisionPedido();
        revision.setPedido(pedido);
        revision.setMotivoRetencion(limitarTexto(
                motivo == null || motivo.isBlank()
                        ? "Retención manual por reglas de negocio."
                        : motivo,
                255
        ));
        revision.setResuelto(false);
        revisionPedidoRepository.save(revision);
    }

    private void registrarAuditoriaAlta(Pedido pedido, Integer idOperario, String observacionGeo) {
        Operario operario = operarioRepository.getReferenceById(idOperario);

        StringBuilder detalle = new StringBuilder()
                .append("Alta de pedido ")
                .append(pedido.getCodigoPedido())
                .append(". Estado inicial: ")
                .append(pedido.getEstado());

        if (observacionGeo != null && !observacionGeo.isBlank()) {
            detalle.append(". Validación geográfica: ").append(observacionGeo.trim());
        }

        AuditoriaLog log = new AuditoriaLog();
        log.setOperario(operario);
        log.setPedido(pedido);
        log.setTipoAccion("ALTA_PEDIDO");
        log.setDetalleModificacion(limitarTexto(detalle.toString(), MAX_DETALLE_AUDITORIA));
        auditoriaLogRepository.save(log);
    }

    private Map<Integer, String> buscarMotivosRevision(List<Pedido> pedidos) {
        if (pedidos.isEmpty()) {
            return Map.of();
        }

        List<Integer> ids = pedidos.stream().map(Pedido::getIdPedido).toList();
        Map<Integer, String> motivos = new HashMap<>();
        for (RevisionPedidoRepository.MotivoRevisionProjection motivo
                : revisionPedidoRepository.buscarMotivosVigentes(ids)) {
            motivos.putIfAbsent(motivo.getIdPedido(), motivo.getMotivo());
        }
        return motivos;
    }

    private String limitarTexto(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo);
    }

    private String generarCodigoUnico() {
        for (int intento = 0; intento < 12; intento++) {
            int aleatorio = ThreadLocalRandom.current().nextInt(1_000_000);
            String codigo = "MT-" + LocalDate.now().format(FORMATO_CODIGO)
                    + "-" + String.format("%06d", aleatorio);
            if (!pedidoRepository.existsByCodigoPedido(codigo)) {
                return codigo;
            }
        }
        throw new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo generar un código único para el pedido. Intentá nuevamente."
        );
    }

    private PedidoResponse aRespuesta(Pedido pedido, String observacion, String motivoRevision) {
        Cliente cliente = pedido.getCliente();
        boolean direccionIncompleta = Direcciones.esIncompleta(
                pedido.getCalle(),
                pedido.getAltura(),
                pedido.getLocalidad()
        );

        return new PedidoResponse(
                pedido.getIdPedido(),
                pedido.getCodigoPedido(),
                cliente.getIdCliente(),
                cliente.getNombreRazonSocial(),
                cliente.getNumeroDocumento(),
                cliente.getTipoCliente(),
                Boolean.TRUE.equals(cliente.getEsFrecuente()),
                pedido.getDescripcionPaquete(),
                pedido.getPesoKg(),
                pedido.getKilometros(),
                pedido.getPrecioBase(),
                pedido.getRecargo(),
                pedido.getPrecioTotal(),
                pedido.getCalle(),
                pedido.getAltura(),
                pedido.getPisoDepto(),
                pedido.getLocalidad(),
                Direcciones.formatear(
                        pedido.getCalle(),
                        pedido.getAltura(),
                        pedido.getPisoDepto(),
                        pedido.getLocalidad()
                ),
                pedido.getLatitud(),
                pedido.getLongitud(),
                pedido.getEstado(),
                direccionIncompleta,
                pedido.getMotivoFalla(),
                Boolean.TRUE.equals(pedido.getEsReintento()),
                pedido.getFechaCreacion(),
                pedido.getFechaProgramadaEntrega(),
                observacion,
                motivoRevision
        );
    }
}
