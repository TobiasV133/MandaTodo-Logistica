package com.mandatodo.logistica.auditoria;

import java.text.Normalizer;
import java.util.Locale;

public enum TipoEvento {
    ALTA("Alta"),
    EDICION("Edición"),
    REIMPRESION("Reimpresión"),
    CAMBIO_DE_ESTADO("Cambio de Estado");

    private final String valor;

    TipoEvento(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    /**
     * Acepta tanto el nombre del enum (ALTA, CAMBIO_DE_ESTADO) como el valor
     * visible en la UI (Alta, Cambio de Estado). El alias CAMBIO_ESTADO se
     * mantiene por compatibilidad con el filtro del frontend.
     */
    public static TipoEvento desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = normalizar(valor);
        if (normalizado.equals("CAMBIOESTADO")) {
            return CAMBIO_DE_ESTADO;
        }
        for (TipoEvento evento : values()) {
            if (normalizar(evento.name()).equals(normalizado)
                || normalizar(evento.valor).equals(normalizado)) {
                return evento;
            }
        }
        throw new IllegalArgumentException("Tipo de evento inválido: " + valor);
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[\\s_-]", "")
            .toUpperCase(Locale.ROOT);
    }
}
