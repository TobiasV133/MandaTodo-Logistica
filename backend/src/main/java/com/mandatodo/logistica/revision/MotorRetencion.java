package com.mandatodo.logistica.revision;

import com.mandatodo.logistica.cliente.TipoCliente;
import com.mandatodo.logistica.pedido.FiabilidadGeocodificacion;
import com.mandatodo.logistica.shared.texto.TextoNormalizador;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Motor de retención "En Revisión".
 *
 * <p>Reglas implementadas:
 * <ol>
 *   <li>Dirección ambigua: altura nula o 0, calle "S/N" o sin número, calle o
 *       localidad vacía, o geocodificación con fiabilidad media/baja.</li>
 *   <li>Posible duplicado comercial: cliente Empresa con un pedido idéntico
 *       creado el mismo día. La firma compara descripción, peso, monto, calle,
 *       altura, piso/depto y localidad, normalizando mayúsculas y acentos.</li>
 * </ol>
 *
 * <p>Los clientes Particular nunca se retienen por duplicado, tal como indica
 * la especificación. La exención de clientes frecuentes que existe en el
 * prototipo no se aplica acá porque la spec no la define como regla de backend.
 */
@Component
public class MotorRetencion {

    public ResultadoRetencion evaluar(EvaluacionRetencion evaluacion) {
        Objects.requireNonNull(evaluacion, "La evaluación de retención es obligatoria");
        Objects.requireNonNull(evaluacion.pedido(), "El pedido a evaluar es obligatorio");

        Set<AlertaRetencion> alertas = new LinkedHashSet<>();
        evaluarDireccion(evaluacion.pedido(), evaluacion.fiabilidad(), alertas);

        if (evaluacion.tipoCliente() == TipoCliente.Empresa
            && hayDuplicado(evaluacion.pedido(), evaluacion.pedidosDelMismoDia())) {
            alertas.add(AlertaRetencion.DUPLICADO_EMPRESA);
        }

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

    private boolean hayDuplicado(PedidoEvaluable nuevo, List<PedidoEvaluable> candidatos) {
        if (candidatos == null || candidatos.isEmpty()) {
            return false;
        }
        return candidatos.stream().anyMatch(candidato ->
            mismaFecha(nuevo, candidato) && mismaFirma(nuevo, candidato)
        );
    }

    private boolean mismaFecha(PedidoEvaluable nuevo, PedidoEvaluable candidato) {
        if (nuevo.fechaCreacion() == null || candidato.fechaCreacion() == null) {
            return true;
        }
        return nuevo.fechaCreacion().toLocalDate().equals(candidato.fechaCreacion().toLocalDate());
    }

    private boolean mismaFirma(PedidoEvaluable nuevo, PedidoEvaluable candidato) {
        return texto(nuevo.descripcion()).equals(texto(candidato.descripcion()))
            && decimalIgual(nuevo.pesoKg(), candidato.pesoKg())
            && decimalIgual(nuevo.montoEnvio(), candidato.montoEnvio())
            && texto(nuevo.calle()).equals(texto(candidato.calle()))
            && Objects.equals(nuevo.altura(), candidato.altura())
            && texto(nuevo.pisoDepto()).equals(texto(candidato.pisoDepto()))
            && texto(nuevo.localidad()).equals(texto(candidato.localidad()));
    }

    private String texto(String valor) {
        return TextoNormalizador.normalizarParaComparar(valor);
    }

    private boolean decimalIgual(BigDecimal primero, BigDecimal segundo) {
        if (primero == null || segundo == null) {
            return primero == segundo;
        }
        return primero.compareTo(segundo) == 0;
    }
}
