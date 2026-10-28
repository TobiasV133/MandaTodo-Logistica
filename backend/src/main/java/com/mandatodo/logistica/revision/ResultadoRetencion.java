package com.mandatodo.logistica.revision;

import java.util.List;

public record ResultadoRetencion(
    boolean retenido,
    List<MotivoRetencion> motivos,
    List<AlertaRetencion> alertas
) {
    public ResultadoRetencion {
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
    }
}
