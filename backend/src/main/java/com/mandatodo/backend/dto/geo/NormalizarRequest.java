package com.mandatodo.backend.dto.geo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NormalizarRequest(
        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 150, message = "La calle no puede superar los 150 caracteres")
        String calle,

        @Size(max = 15, message = "La altura no puede superar los 15 caracteres")
        String altura,

        @NotBlank(message = "La localidad es obligatoria")
        @Size(max = 120, message = "La localidad no puede superar los 120 caracteres")
        String localidad
) {
}
