package com.mandatodo.backend.model;

/**
 * Motivos válidos para marcar una entrega como fallida.
 */
public enum MotivoFalla {
    AUSENTE,
    DIRECCION_INEXISTENTE,
    DIRECCION_INCOMPLETA;

    /**
     * Valor legible que utiliza el contrato REST.
     */
    public String valorApi() {
        return switch (this) {
            case AUSENTE -> "Ausente";
            case DIRECCION_INEXISTENTE -> "Dirección Inexistente";
            case DIRECCION_INCOMPLETA -> "Dirección Incompleta";
        };
    }

    /**
     * Interpreta el valor recibido por la API.
     */
    public static MotivoFalla desdeApi(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El motivo de falla es obligatorio.");
        }

        return switch (valor.trim()) {
            case "Ausente", "AUSENTE" -> AUSENTE;
            case "Dirección Inexistente", "Direccion Inexistente", "DIRECCION_INEXISTENTE" ->
                    DIRECCION_INEXISTENTE;
            case "Dirección Incompleta", "Direccion Incompleta", "DIRECCION_INCOMPLETA" ->
                    DIRECCION_INCOMPLETA;
            default -> throw new IllegalArgumentException("Motivo de falla inválido: " + valor);
        };
    }
}
