package com.mandatodo.logistica.revision;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum AccionRevision {
    APROBAR,
    RECHAZAR;

    @JsonCreator
    public static AccionRevision desde(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        for (AccionRevision accion : values()) {
            if (accion.name().equals(normalizado)) {
                return accion;
            }
        }
        throw new IllegalArgumentException("Acción de revisión inválida: " + valor);
    }
}
