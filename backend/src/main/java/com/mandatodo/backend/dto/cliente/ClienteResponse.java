package com.mandatodo.backend.dto.cliente;

import com.mandatodo.backend.model.Cliente;
import com.mandatodo.backend.model.TipoCliente;
import com.mandatodo.backend.model.TipoDocumento;

import java.time.LocalDateTime;

public record ClienteResponse(
        Integer idCliente,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombreRazonSocial,
        String telefono,
        String email,
        TipoCliente tipoCliente,
        Boolean esFrecuente,
        LocalDateTime fechaCreacion
) {
    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(
                cliente.getIdCliente(),
                cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(),
                cliente.getNombreRazonSocial(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getTipoCliente(),
                Boolean.TRUE.equals(cliente.getEsFrecuente()),
                cliente.getFechaCreacion()
        );
    }
}
