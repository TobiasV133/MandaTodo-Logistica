package com.mandatodo.logistica.auditoria;

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

    public static TipoEvento desde(String valor) {
        if (valor == null) {
            return null;
        }
        for (TipoEvento evento : values()) {
            if (evento.valor.equalsIgnoreCase(valor.trim())) {
                return evento;
            }
        }
        throw new IllegalArgumentException("Tipo de evento inválido: " + valor);
    }
}
