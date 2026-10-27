package com.mandatodo.backend.dto;

import com.mandatodo.backend.model.TipoCliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos editables de un cliente. El documento no se modifica por esta vía.
 */
public record ActualizarClienteDTO(

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
