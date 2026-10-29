package com.mandatodo.logistica.cliente.dto;

import com.mandatodo.logistica.cliente.TipoCliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearClienteDTO(
    @NotBlank(message = "El tipo de documento es obligatorio")
    String tipoDocumento,

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 30, message = "El número de documento no puede superar los 30 caracteres")
    String numeroDocumento,

    @NotBlank(message = "El nombre o razón social es obligatorio")
    @Size(max = 150, message = "El nombre o razón social no puede superar los 150 caracteres")
    String nombreRazonSocial,

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    String telefono,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    String email,

    @NotNull(message = "El tipo de cliente es obligatorio")
    TipoCliente tipoCliente,

    Boolean esFrecuente
) {
}
