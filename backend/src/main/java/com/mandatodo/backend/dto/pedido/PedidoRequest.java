package com.mandatodo.backend.dto.pedido;

import com.mandatodo.backend.dto.cliente.ClienteRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PedidoRequest(
        @NotNull(message = "Los datos del cliente son obligatorios")
        @Valid
        ClienteRequest cliente,

        @NotBlank(message = "La descripción del paquete es obligatoria")
        @Size(max = 300, message = "La descripción no puede superar los 300 caracteres")
        String descripcionPaquete,

        @NotNull(message = "El peso es obligatorio")
        @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0")
        @Digits(integer = 7, fraction = 3, message = "El peso tiene un formato inválido")
        BigDecimal pesoKg,

        @NotNull(message = "Los kilómetros son obligatorios")
        @DecimalMin(value = "0.00", message = "Los kilómetros no pueden ser negativos")
        @Digits(integer = 8, fraction = 2, message = "Los kilómetros tienen un formato inválido")
        BigDecimal kilometros,

        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 150, message = "La calle no puede superar los 150 caracteres")
        String calle,

        @PositiveOrZero(message = "La altura no puede ser negativa")
        Integer altura,

        @Size(max = 40, message = "El piso/depto no puede superar los 40 caracteres")
        String pisoDepto,

        @NotBlank(message = "La localidad es obligatoria")
        @Size(max = 120, message = "La localidad no puede superar los 120 caracteres")
        String localidad,

        @NotNull(message = "La dirección debe estar validada geográficamente")
        @DecimalMin(value = "-90.0", message = "La latitud no es válida")
        @DecimalMax(value = "90.0", message = "La latitud no es válida")
        BigDecimal latitud,

        @NotNull(message = "La dirección debe estar validada geográficamente")
        @DecimalMin(value = "-180.0", message = "La longitud no es válida")
        @DecimalMax(value = "180.0", message = "La longitud no es válida")
        BigDecimal longitud,

        Boolean esAmbigua,

        @Size(max = 300, message = "La observación geográfica no puede superar los 300 caracteres")
        String observacionGeo,

        LocalDate fechaProgramadaEntrega,

        Integer idPedidoOrigen
) {
}
