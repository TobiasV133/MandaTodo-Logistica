package com.mandatodo.logistica.revision;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MotivoRetencionConverter implements AttributeConverter<MotivoRetencion, String> {

    @Override
    public String convertToDatabaseColumn(MotivoRetencion atributo) {
        return atributo == null ? null : atributo.getValor();
    }

    @Override
    public MotivoRetencion convertToEntityAttribute(String valor) {
        return valor == null ? null : MotivoRetencion.desde(valor);
    }
}
