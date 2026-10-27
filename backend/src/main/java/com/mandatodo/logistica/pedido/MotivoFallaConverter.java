package com.mandatodo.logistica.pedido;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MotivoFallaConverter implements AttributeConverter<MotivoFalla, String> {

    @Override
    public String convertToDatabaseColumn(MotivoFalla atributo) {
        return atributo == null ? null : atributo.getValor();
    }

    @Override
    public MotivoFalla convertToEntityAttribute(String valor) {
        return valor == null ? null : MotivoFalla.desde(valor);
    }
}
