package com.mandatodo.logistica.cliente.dto;

import com.mandatodo.logistica.cliente.Cliente;
import com.mandatodo.logistica.cliente.TipoCliente;

public record ClienteDTO(
    Long idCliente,
    String tipoDocumento,
    String numeroDocumento,
    String nombreRazonSocial,
    String telefono,
    String email,
    TipoCliente tipoCliente,
    Boolean esFrecuente
) {
    public static ClienteDTO desde(Cliente cliente) {
        return new ClienteDTO(
            cliente.getId(),
            cliente.getTipoDocumento(),
            cliente.getNumeroDocumento(),
            cliente.getNombreRazonSocial(),
            cliente.getTelefono(),
            cliente.getEmail(),
            cliente.getTipoCliente(),
            cliente.isEsFrecuente()
        );
    }
}
