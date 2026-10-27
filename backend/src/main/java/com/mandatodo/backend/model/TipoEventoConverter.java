package com.mandatodo.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convierte {@link TipoEvento} a los literales exactos del ENUM de MySQL.
 */
@Converter
public class TipoEventoConverter implements AttributeConverter<TipoEvento, String> {

    @Override
    public String convertToDatabaseColumn(TipoEvento atributo) {
        return atributo == null ? null : atributo.valorApi();
    }

    @Override
    public TipoEvento convertToEntityAttribute(String valorDb) {
        if (valorDb == null) {
            return null;
        }

        try {
            return TipoEvento.desdeApi(valorDb);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Tipo de evento de auditoría desconocido: " + valorDb,
                    ex
            );
        }
    }
}
