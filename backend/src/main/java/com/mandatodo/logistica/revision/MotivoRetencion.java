package com.mandatodo.logistica.revision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mandatodo.logistica.shared.texto.TextoNormalizador;

public enum MotivoRetencion {
    DIRECCION_AMBIGUA("Dirección ambigua"),
    POSIBLE_DUPLICADO_LOCAL("Posible duplicado de local");

    private final String valor;

    MotivoRetencion(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static MotivoRetencion desde(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = TextoNormalizador.normalizar(valor);
        for (MotivoRetencion motivo : values()) {
            if (TextoNormalizador.normalizar(motivo.valor).equals(normalizado)
                || motivo.name().equalsIgnoreCase(valor.trim())) {
                return motivo;
            }
        }
        throw new IllegalArgumentException("Motivo de retención inválido: " + valor);
    }
}
