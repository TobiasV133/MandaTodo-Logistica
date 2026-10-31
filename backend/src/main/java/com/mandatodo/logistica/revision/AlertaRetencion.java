package com.mandatodo.logistica.revision;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Alertas que explican una retención. Se devuelven en la respuesta de alta para
 * que la interfaz pueda mostrar el mismo detalle que usa el prototipo, aunque
 * la tabla revisiones solo persiste el motivo de retención.
 */
public enum AlertaRetencion {
    ALTURA_CERO("altura_cero", MotivoRetencion.DIRECCION_AMBIGUA),
    SIN_NUMERO("sin_numero", MotivoRetencion.DIRECCION_AMBIGUA),
    GEO_AMBIGUA("geo_ambigua", MotivoRetencion.DIRECCION_AMBIGUA),
    FIABILIDAD_BAJA("fiabilidad_baja", MotivoRetencion.DIRECCION_AMBIGUA),
    FIABILIDAD_MEDIA("fiabilidad_media", MotivoRetencion.DIRECCION_AMBIGUA),
    DUPLICADO_LOCAL("duplicado_local", MotivoRetencion.POSIBLE_DUPLICADO_LOCAL);

    private final String codigo;
    private final MotivoRetencion motivo;

    AlertaRetencion(String codigo, MotivoRetencion motivo) {
        this.codigo = codigo;
        this.motivo = motivo;
    }

    @JsonValue
    public String getCodigo() {
        return codigo;
    }

    public MotivoRetencion getMotivo() {
        return motivo;
    }
}
