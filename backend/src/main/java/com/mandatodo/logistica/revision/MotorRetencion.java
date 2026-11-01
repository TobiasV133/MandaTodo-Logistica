package com.mandatodo.logistica.revision;

import com.mandatodo.logistica.pedido.FiabilidadGeocodificacion;
import com.mandatodo.logistica.shared.texto.TextoNormalizador;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Motor de retención "En Revisión" por dirección.
 *
 * <p>Reglas implementadas:
 * <ol>
 *   <li>Dirección ambigua: altura nula o 0, calle "S/N" o sin número, calle o
 *       localidad vacía, o geocodificación con fiabilidad media/baja.</li>
 * </ol>
 *
 * <p>El duplicado del día no se evalúa acá: lo resuelve
 * {@code PedidoService#validarYGuardarPedido(Pedido)} consultando la base
 * (mismo cliente y misma fecha). Esta separación evita dos fuentes de verdad
 * para la misma regla de negocio.
 */
@Component
public class MotorRetencion {

    public ResultadoRetencion evaluar(EvaluacionRetencion evaluacion) {
        Objects.requireNonNull(evaluacion, "La evaluación de retención es obligatoria");
        Objects.requireNonNull(evaluacion.pedido(), "El pedido a evaluar es obligatorio");

        Set<AlertaRetencion> alertas = new LinkedHashSet<>();
        evaluarDireccion(evaluacion.pedido(), evaluacion.fiabilidad(), alertas);

        List<MotivoRetencion> motivos = alertas.stream()
            .map(AlertaRetencion::getMotivo)
            .distinct()
            .toList();

        return new ResultadoRetencion(!motivos.isEmpty(), motivos, List.copyOf(alertas));
    }

    private void evaluarDireccion(
        PedidoEvaluable pedido,
        FiabilidadGeocodificacion fiabilidad,
        Set<AlertaRetencion> alertas
    ) {
        boolean calleVacia = !StringUtils.hasText(pedido.calle());
        boolean localidadVacia = !StringUtils.hasText(pedido.localidad());

        if (pedido.altura() == null || pedido.altura() <= 0) {
            alertas.add(AlertaRetencion.ALTURA_CERO);
        }
        if (calleVacia || TextoNormalizador.esCalleSinNumero(pedido.calle())) {
            alertas.add(AlertaRetencion.SIN_NUMERO);
        }
        if (calleVacia || localidadVacia) {
            alertas.add(AlertaRetencion.GEO_AMBIGUA);
        }
        if (fiabilidad == FiabilidadGeocodificacion.BAJA) {
            alertas.add(AlertaRetencion.FIABILIDAD_BAJA);
        } else if (fiabilidad == FiabilidadGeocodificacion.MEDIA) {
            alertas.add(AlertaRetencion.FIABILIDAD_MEDIA);
        }
    }
}
