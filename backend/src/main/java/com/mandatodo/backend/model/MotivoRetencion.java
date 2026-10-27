package com.mandatodo.backend.model;

/**
 * Motivos por los que un pedido queda retenido en la bandeja de revisión.
 */
public enum MotivoRetencion {
    DIRECCION_AMBIGUA,
    POSIBLE_DUPLICADO_COMERCIAL;

    /**
     * Valor legible que utiliza el contrato REST.
     */
    public String valorApi() {
        return switch (this) {
            case DIRECCION_AMBIGUA -> "Dirección ambigua";
            case POSIBLE_DUPLICADO_COMERCIAL -> "Posible duplicado comercial";
        };
    }

    /**
     * Interpreta el valor recibido por la API.
     */
    public static MotivoRetencion desdeApi(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El motivo de retención es obligatorio.");
        }

        return switch (valor.trim()) {
            case "Dirección ambigua", "Direccion ambigua", "DIRECCION_AMBIGUA" ->
                    DIRECCION_AMBIGUA;
            case "Posible duplicado comercial", "POSIBLE_DUPLICADO_COMERCIAL" ->
                    POSIBLE_DUPLICADO_COMERCIAL;
            default -> throw new IllegalArgumentException("Motivo de retención inválido: " + valor);
        };
    }
}
