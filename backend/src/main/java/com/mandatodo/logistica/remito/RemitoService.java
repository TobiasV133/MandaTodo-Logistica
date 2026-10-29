package com.mandatodo.logistica.remito;

import com.mandatodo.logistica.auditoria.AuditoriaRepository;
import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.pedido.EntregaFallidaService;
import com.mandatodo.logistica.pedido.EstadoPedido;
import com.mandatodo.logistica.pedido.Pedido;
import com.mandatodo.logistica.pedido.PedidoRepository;
import com.mandatodo.logistica.remito.dto.ImprimirRemitoRequest;
import com.mandatodo.logistica.shared.error.RecursoNoEncontradoException;
import com.mandatodo.logistica.shared.error.ReglaNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Orquesta la emisión del remito: valida el pedido, decide si es una primera impresión o una
 * reimpresión, genera el PDF y registra el evento en la auditoría inalterable.
 *
 * <p>El esquema MySQL no tiene una columna {@code remito_generado}; la propia auditoría es la
 * fuente de verdad. Un pedido ya tiene remito cuando existe un evento cuyo texto empieza con
 * {@value #PREFIJO_ACTIVIDAD_REMITO}. A partir de la segunda impresión se exige el motivo, que se
 * guarda en el texto del evento con tipo {@code Reimpresión}.</p>
 *
 * <p>Si la auditoría registra entregas fallidas, el remito separa el subtotal original del recargo
 * acumulado del 50%. El total sigue siendo el {@code monto_envio} persistido.</p>
 */
@Service
public class RemitoService {

    public static final String PREFIJO_ACTIVIDAD_REMITO = "Remito ";

    private static final String ACTIVIDAD_ENTREGA_FALLIDA = "marcado como FALLIDO";
    private static final BigDecimal FACTOR_RECARGO = BigDecimal.ONE.add(
        EntregaFallidaService.PORCENTAJE_RECARGO
    );

    private static final Logger log = LoggerFactory.getLogger(RemitoService.class);

    private final PedidoRepository pedidoRepository;
    private final OperarioRepository operarioRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final AuditoriaService auditoriaService;
    private final RemitoPdfGenerator generador;
    private final EmpresaRemitoProperties empresaProperties;
    private final Clock clock;

    public RemitoService(
        PedidoRepository pedidoRepository,
        OperarioRepository operarioRepository,
        AuditoriaRepository auditoriaRepository,
        AuditoriaService auditoriaService,
        RemitoPdfGenerator generador,
        EmpresaRemitoProperties empresaProperties,
        Clock clock
    ) {
        this.pedidoRepository = pedidoRepository;
        this.operarioRepository = operarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.auditoriaService = auditoriaService;
        this.generador = generador;
        this.empresaProperties = empresaProperties;
        this.clock = clock;
    }

    @Transactional
    public RemitoGenerado imprimir(Long idPedido, ImprimirRemitoRequest request) {
        return imprimir(idPedido, request, request.operarioId());
    }

    @Transactional
    public RemitoGenerado imprimir(
        Long idPedido,
        ImprimirRemitoRequest request,
        Long operarioId
    ) {
        Pedido pedido = pedidoRepository.findConClientePorId(idPedido)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No se encontró el pedido " + idPedido
            ));
        Operario operario = buscarOperario(operarioId);

        validarEstado(pedido);

        boolean reimpresion = yaTieneRemito(idPedido);
        String motivo = normalizar(request.motivoReimpresion());

        if (reimpresion && motivo == null) {
            throw new ReglaNegocioException(
                "El motivo de reimpresión es obligatorio: el remito "
                    + DatosRemito.numeroDe(idPedido) + " ya fue generado antes"
            );
        }
        if (!reimpresion) {
            motivo = null;
        }

        BigDecimal subtotal = pedido.getMontoEnvio();
        BigDecimal recargo = BigDecimal.ZERO;
        long entregasFallidas = auditoriaRepository.countByPedido_IdAndActividadContaining(
            idPedido,
            ACTIVIDAD_ENTREGA_FALLIDA
        );
        if (entregasFallidas > 0) {
            BigDecimal factor = FACTOR_RECARGO.pow((int) entregasFallidas);
            subtotal = pedido.getMontoEnvio().divide(factor, 2, RoundingMode.HALF_UP);
            recargo = pedido.getMontoEnvio().subtract(subtotal);
        }

        DatosRemito datos = DatosRemito.desde(
            pedido,
            operario,
            resolverKilometros(pedido, request),
            subtotal,
            recargo,
            reimpresion,
            motivo,
            LocalDateTime.now(clock),
            DatosRemito.DatosEmpresa.desde(empresaProperties)
        );

        byte[] contenido = generador.generar(datos);
        auditoriaService.registrar(
            operario,
            pedido,
            actividad(pedido, reimpresion, motivo),
            reimpresion ? TipoEvento.REIMPRESION : TipoEvento.ALTA
        );

        log.info(
            "Remito {} {} para el pedido {} por el operario {}",
            datos.numero(),
            reimpresion ? "reimpreso" : "generado",
            pedido.getId(),
            operario.getId()
        );

        return new RemitoGenerado(
            contenido,
            "remito-" + datos.numero() + ".pdf",
            datos.numero(),
            reimpresion,
            motivo
        );
    }

    /**
     * El operario puede corregir la distancia al imprimir; si no lo hace se usa la que quedó
     * guardada cuando se dio de alta el pedido.
     */
    private BigDecimal resolverKilometros(Pedido pedido, ImprimirRemitoRequest request) {
        return request.kilometros() == null ? pedido.getKilometros() : request.kilometros();
    }

    private Operario buscarOperario(Long operarioId) {
        if (operarioId == null) {
            throw new ReglaNegocioException(
                "No se pudo identificar al operario que imprime el remito."
            );
        }
        return operarioRepository.findById(operarioId)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No se encontró el operario " + operarioId
            ));
    }

    private boolean yaTieneRemito(Long idPedido) {
        return auditoriaRepository.countByPedido_IdAndActividadStartingWith(
            idPedido,
            PREFIJO_ACTIVIDAD_REMITO
        ) > 0;
    }

    private void validarEstado(Pedido pedido) {
        if (pedido.getEstado() == EstadoPedido.EN_REVISION) {
            throw new ReglaNegocioException(
                "El pedido está en revisión: resolvé la revisión antes de generar el remito"
            );
        }
        if (pedido.getEstado() == EstadoPedido.CANCELADO_EN_TRANSITO) {
            throw new ReglaNegocioException(
                "No se puede generar un remito de un pedido cancelado en tránsito"
            );
        }
    }

    private String actividad(Pedido pedido, boolean reimpresion, String motivo) {
        String numero = DatosRemito.numeroDe(pedido.getId());
        StringBuilder texto = new StringBuilder();
        if (reimpresion) {
            texto.append("Remito reimpreso para el pedido #")
                .append(pedido.getId())
                .append(" (")
                .append(numero)
                .append("); motivo: ")
                .append(motivo);
        } else {
            texto.append("Remito generado para el pedido #")
                .append(pedido.getId())
                .append(" (")
                .append(numero)
                .append(")");
        }
        return texto.toString();
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
