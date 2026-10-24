package com.mandatodo.logistica.pedido.dto;

import com.mandatodo.logistica.pedido.FiabilidadGeocodificacion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CrearPedidoRequest(
    @NotNull(message = "es obligatorio")
    Long operarioId,

    @NotNull(message = "es obligatorio")
    Long clienteId,

    @NotBlank(message = "es obligatoria")
    @Size(max = 5000, message = "no puede superar los 5000 caracteres")
    String descripcion,

    @NotNull(message = "es obligatorio")
    @DecimalMin(value = "0.00", message = "no puede ser negativo")
    BigDecimal pesoKg,

    @NotNull(message = "es obligatorio")
    @DecimalMin(value = "0.00", message = "no puede ser negativo")
    BigDecimal montoEnvio,

    @NotBlank(message = "es obligatoria")
    @Size(max = 200, message = "no puede superar los 200 caracteres")
    String calle,

    @NotNull(message = "es obligatoria")
    @Min(value = 0, message = "no puede ser negativa")
    Integer altura,

    @Size(max = 50, message = "no puede superar los 50 caracteres")
    String pisoDepto,

    @NotBlank(message = "es obligatoria")
    @Size(max = 150, message = "no puede superar los 150 caracteres")
    String localidad,

    @DecimalMin(value = "-90.0", message = "debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "debe estar entre -90 y 90")
    BigDecimal latitud,

    @DecimalMin(value = "-180.0", message = "debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "debe estar entre -180 y 180")
    BigDecimal longitud,

    LocalDate fechaProgramada,

    FiabilidadGeocodificacion fiabilidadGeocodificacion
) {
}
