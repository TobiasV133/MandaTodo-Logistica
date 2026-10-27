package com.mandatodo.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convierte {@link MotivoFalla} a los literales exactos del ENUM de MySQL.
 */
@Converter
public class MotivoFallaConverter implements AttributeConverter<MotivoFalla, String> {

    @Override
    public String convertToDatabaseColumn(MotivoFalla atributo) {
        return atributo == null ? null : atributo.valorApi();
    }

    @Override
    public MotivoFalla convertToEntityAttribute(String valorDb) {
        if (valorDb == null) {
            return null;
        }

        try {
            return MotivoFalla.desdeApi(valorDb);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Motivo de falla desconocido: " + valorDb, ex);
        }
    }
}
