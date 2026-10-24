package com.mandatodo.logistica.auditoria;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TipoEventoConverter implements AttributeConverter<TipoEvento, String> {

    @Override
    public String convertToDatabaseColumn(TipoEvento atributo) {
        return atributo == null ? null : atributo.getValor();
    }

    @Override
    public TipoEvento convertToEntityAttribute(String valor) {
        return valor == null ? null : TipoEvento.desde(valor);
    }
}
