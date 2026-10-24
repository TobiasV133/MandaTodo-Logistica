package com.mandatodo.logistica.pedido;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/**
 * Confiabilidad del resultado de geocodificación, informada por el cliente de
 * la API. Si no se informa, el motor solo evalúa la estructura de la dirección.
 */
public enum FiabilidadGeocodificacion {
    ALTA,
    MEDIA,
    BAJA;

    @JsonCreator
    public static FiabilidadGeocodificacion desde(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        for (FiabilidadGeocodificacion fiabilidad : values()) {
            if (fiabilidad.name().equals(normalizado)) {
                return fiabilidad;
            }
        }
        throw new IllegalArgumentException("Fiabilidad de geocodificación inválida: " + valor);
    }
}
