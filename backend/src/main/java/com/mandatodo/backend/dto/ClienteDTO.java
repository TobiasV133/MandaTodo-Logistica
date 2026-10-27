package com.mandatodo.backend.dto;

import com.mandatodo.backend.model.Cliente;
import com.mandatodo.backend.model.TipoCliente;

/**
 * Datos de salida de un cliente.
 */
public record ClienteDTO(
        Integer idCliente,
        String tipoDocumento,
        String numeroDocumento,
        String nombreRazonSocial,
        String telefono,
        String email,
        TipoCliente tipoCliente,
        Boolean esFrecuente
) {

    public static ClienteDTO from(Cliente cliente) {
        return new ClienteDTO(
                cliente.getIdCliente(),
                cliente.getTipoDocumento() == null ? null : cliente.getTipoDocumento().name(),
                cliente.getNumeroDocumento(),
                cliente.getNombreRazonSocial(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getTipoCliente(),
                Boolean.TRUE.equals(cliente.getEsFrecuente())
        );
    }
}
