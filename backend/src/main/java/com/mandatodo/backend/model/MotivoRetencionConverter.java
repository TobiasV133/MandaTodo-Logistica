package com.mandatodo.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convierte {@link MotivoRetencion} a los literales exactos del ENUM de MySQL.
 */
@Converter
public class MotivoRetencionConverter implements AttributeConverter<MotivoRetencion, String> {

    @Override
    public String convertToDatabaseColumn(MotivoRetencion atributo) {
        return atributo == null ? null : atributo.valorApi();
    }

    @Override
    public MotivoRetencion convertToEntityAttribute(String valorDb) {
        if (valorDb == null) {
            return null;
        }

        try {
            return MotivoRetencion.desdeApi(valorDb);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Motivo de retención desconocido: " + valorDb, ex);
        }
    }
}
