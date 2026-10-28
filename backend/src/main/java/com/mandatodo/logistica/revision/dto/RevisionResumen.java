package com.mandatodo.logistica.revision.dto;

import com.mandatodo.logistica.revision.EstadoResolucion;
import com.mandatodo.logistica.revision.MotivoRetencion;

public record RevisionResumen(
    Long idRevision,
    MotivoRetencion motivoRetencion,
    EstadoResolucion estadoResolucion
) {
}
