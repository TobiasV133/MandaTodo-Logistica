package com.mandatodo.logistica.pedido;

import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.shared.calendario.CalendarioHabil;
import com.mandatodo.logistica.shared.error.ReglaNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Lógica de entregas fallidas:
 * <ul>
 *   <li>exige un motivo de falla;</li>
 *   <li>reprograma el pedido para el siguiente día hábil;</li>
 *   <li>aplica un recargo del 50% sobre el costo de envío vigente.</li>
 * </ul>
 *
 * <p>El monto con recargo se persiste en {@code pedidos.monto_envio}, tal como
 * reflejan los datos de prueba de {@code seeds.sql}. Si un pedido se reprograma
 * y vuelve a fallar, el 50% se calcula sobre el monto vigente en ese momento.
 */
@Service
public class EntregaFallidaService {

    public static final BigDecimal PORCENTAJE_RECARGO = new BigDecimal("0.50");

    private static final Logger log = LoggerFactory.getLogger(EntregaFallidaService.class);

    private final Clock clock;
    private final CalendarioHabil calendarioHabil;
    private final AuditoriaService auditoriaService;

    public EntregaFallidaService(
        Clock clock,
        CalendarioHabil calendarioHabil,
        AuditoriaService auditoriaService
    ) {
        this.clock = clock;
        this.calendarioHabil = calendarioHabil;
        this.auditoriaService = auditoriaService;
    }

    public ResultadoReprogramacion registrarFallo(
        Pedido pedido,
        MotivoFalla motivoFalla,
        String notaFalla,
        Operario operario
    ) {
        if (pedido == null) {
            throw new IllegalArgumentException("El pedido es obligatorio");
        }
        if (motivoFalla == null) {
            throw new ReglaNegocioException(
                "El motivo de falla es obligatorio para marcar un pedido como FALLIDO"
            );
        }
        if (pedido.getEstado() == EstadoPedido.FALLIDO) {
            throw new ReglaNegocioException("El pedido ya está marcado como FALLIDO");
        }
        if (pedido.getEstado() == EstadoPedido.ENTREGADO
            || pedido.getEstado() == EstadoPedido.CANCELADO_EN_TRANSITO) {
            throw new ReglaNegocioException(
                "No se puede marcar como FALLIDO un pedido en estado " + pedido.getEstado()
            );
        }

        BigDecimal montoAnterior = pedido.getMontoEnvio();
        BigDecimal recargo = montoAnterior
            .multiply(PORCENTAJE_RECARGO)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoFinal = montoAnterior
            .add(recargo)
            .setScale(2, RoundingMode.HALF_UP);

        LocalDate hoy = LocalDate.now(clock);
        LocalDate referencia = pedido.getFechaProgramada() != null
            && pedido.getFechaProgramada().isAfter(hoy)
                ? pedido.getFechaProgramada()
                : hoy;
        LocalDate fechaReprogramada = calendarioHabil.siguienteDiaHabil(referencia);

        pedido.setMontoEnvio(montoFinal);
        pedido.setFechaProgramada(fechaReprogramada);
        pedido.setEstado(EstadoPedido.FALLIDO);
        pedido.setMotivoFalla(motivoFalla);
        pedido.setMotivoCancelacion(null);

        String actividad = "Pedido #%d marcado como FALLIDO por %s; reprogramado para %s "
            .formatted(pedido.getId(), motivoFalla.getValor(), fechaReprogramada)
            + "con recargo del 50%% sobre el costo base ($%s)".formatted(recargo);
        if (StringUtils.hasText(notaFalla)) {
            actividad += ". Nota: " + notaFalla.trim();
        }

        auditoriaService.registrar(
            operario,
            pedido,
            actividad,
            TipoEvento.CAMBIO_DE_ESTADO
        );

        log.info(
            "Pedido {} marcado como FALLIDO por {}; recargo {}; reprogramado para {}",
            pedido.getId(),
            motivoFalla.getValor(),
            recargo,
            fechaReprogramada
        );

        return new ResultadoReprogramacion(recargo, fechaReprogramada, montoFinal, montoAnterior);
    }
}
