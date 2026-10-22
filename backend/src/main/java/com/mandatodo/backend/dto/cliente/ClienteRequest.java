package com.mandatodo.backend.dto.cliente;

import com.mandatodo.backend.model.TipoCliente;
import com.mandatodo.backend.model.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotNull(message = "El tipo de documento es obligatorio")
        TipoDocumento tipoDocumento,

        @NotBlank(message = "El número de documento es obligatorio")
        @Pattern(regexp = "^[0-9.\\-\\s]{7,20}$", message = "El número de documento no es válido")
        String numeroDocumento,

        @NotBlank(message = "El nombre o razón social es obligatorio")
        @Size(max = 180, message = "El nombre o razón social no puede superar los 180 caracteres")
        String nombreRazonSocial,

        @Size(max = 40, message = "El teléfono no puede superar los 40 caracteres")
        String telefono,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 180, message = "El email no puede superar los 180 caracteres")
        String email,

        @NotNull(message = "El tipo de cliente es obligatorio")
        TipoCliente tipoCliente,

        Boolean esFrecuente
) {
}
