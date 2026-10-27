package com.mandatodo.logistica.pedido;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mandatodo.logistica.shared.texto.TextoNormalizador;

public enum MotivoFalla {
    AUSENTE("Ausente"),
    DIRECCION_INEXISTENTE("Dirección Inexistente"),
    DIRECCION_INCOMPLETA("Dirección Incompleta");

    private final String valor;

    MotivoFalla(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static MotivoFalla desde(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = TextoNormalizador.normalizar(valor);
        for (MotivoFalla motivo : values()) {
            if (TextoNormalizador.normalizar(motivo.valor).equals(normalizado)
                || motivo.name().equalsIgnoreCase(valor.trim())) {
                return motivo;
            }
        }
        throw new IllegalArgumentException("Motivo de falla inválido: " + valor);
    }
}
