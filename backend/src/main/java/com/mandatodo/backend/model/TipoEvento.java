package com.mandatodo.backend.model;

/**
 * Tipos de evento registrados en la auditoría global.
 *
 * <p>Los valores persistidos y expuestos por la API incluyen espacios y tildes,
 * por lo que la conversión se realiza con {@link TipoEventoConverter} y con
 * {@link #valorApi()} / {@link #desdeApi(String)}.</p>
 */
public enum TipoEvento {
    ALTA,
    EDICION,
    REIMPRESION,
    CAMBIO_DE_ESTADO;

    /**
     * Valor legible que utiliza el contrato REST.
     */
    public String valorApi() {
        return switch (this) {
            case ALTA -> "Alta";
            case EDICION -> "Edición";
            case REIMPRESION -> "Reimpresión";
            case CAMBIO_DE_ESTADO -> "Cambio de Estado";
        };
    }

    /**
     * Interpreta el valor recibido por la API, aceptando también el nombre
     * técnico del enum.
     */
    public static TipoEvento desdeApi(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El tipo de evento es obligatorio.");
        }

        return switch (valor.trim()) {
            case "Alta", "ALTA" -> ALTA;
            case "Edición", "Edicion", "EDICION" -> EDICION;
            case "Reimpresión", "Reimpresion", "REIMPRESION" -> REIMPRESION;
            case "Cambio de Estado", "CambioDeEstado", "CAMBIO_DE_ESTADO" -> CAMBIO_DE_ESTADO;
            default -> throw new IllegalArgumentException(
                    "Tipo de evento inválido: " + valor
            );
        };
    }
}
