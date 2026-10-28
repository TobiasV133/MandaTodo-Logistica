package com.mandatodo.logistica.cliente.dto;

import com.mandatodo.logistica.cliente.Cliente;
import com.mandatodo.logistica.cliente.TipoCliente;
import java.time.LocalDateTime;

/**
 * fechaCreacion se conserva como null para el contrato del frontend de main;
 * el esquema canónico de clientes no guarda esa fecha.
 */
public record ClienteResponse(
    Long idCliente,
    String tipoDocumento,
    String numeroDocumento,
    String nombreRazonSocial,
    String telefono,
    String email,
    TipoCliente tipoCliente,
    Boolean esFrecuente,
    LocalDateTime fechaCreacion
) {
    public static ClienteResponse desde(Cliente cliente) {
        return new ClienteResponse(
            cliente.getId(),
            cliente.getTipoDocumento(),
            cliente.getNumeroDocumento(),
            cliente.getNombreRazonSocial(),
            cliente.getTelefono(),
            cliente.getEmail(),
            cliente.getTipoCliente(),
            cliente.isEsFrecuente(),
            null
        );
    }
}
