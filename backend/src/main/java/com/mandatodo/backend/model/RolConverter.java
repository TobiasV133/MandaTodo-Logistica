package com.mandatodo.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * La tabla operarios de mandatodo_db persiste los roles como 'Admin' y 'Operario'.
 * El dominio mantiene los nombres en mayúsculas y este converter realiza el mapeo estricto.
 */
@Converter
public class RolConverter implements AttributeConverter<Rol, String> {

    @Override
    public String convertToDatabaseColumn(Rol rol) {
        if (rol == null) {
            return null;
        }
        return switch (rol) {
            case ADMINISTRADOR -> "Admin";
            case OPERARIO -> "Operario";
        };
    }

    @Override
    public Rol convertToEntityAttribute(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return switch (valor.trim()) {
            case "Admin", "ADMINISTRADOR" -> Rol.ADMINISTRADOR;
            case "Operario", "OPERARIO" -> Rol.OPERARIO;
            default -> throw new IllegalArgumentException("Rol desconocido en la base de datos: " + valor);
        };
    }
}
