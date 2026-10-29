package com.mandatodo.logistica.pedido.dto;

import com.mandatodo.logistica.cliente.dto.ClienteRequest;
import com.mandatodo.logistica.pedido.FiabilidadGeocodificacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrato de alta de pedido. Acepta tanto el payload canónico (clienteId,
 * descripcion, montoEnvio) como el del frontend de main (cliente anidado,
 * descripcionPaquete, kilometros y esAmbigua).
 */
public record CrearPedidoRequest(
    Long operarioId,

    Long clienteId,

    @Size(max = 5000, message = "no puede superar los 5000 caracteres")
    String descripcion,

    @NotNull(message = "es obligatorio")
    @DecimalMin(value = "0.00", message = "no puede ser negativo")
    BigDecimal pesoKg,

    @DecimalMin(value = "0.00", message = "no puede ser negativo")
    BigDecimal montoEnvio,

    @NotBlank(message = "es obligatoria")
    @Size(max = 200, message = "no puede superar los 200 caracteres")
    String calle,

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

    FiabilidadGeocodificacion fiabilidadGeocodificacion,

    @Valid
    ClienteRequest cliente,

    @Size(max = 5000, message = "no puede superar los 5000 caracteres")
    String descripcionPaquete,

    @NotNull(message = "Los kilómetros son obligatorios para calcular el envío")
    @DecimalMin(value = "0.00", message = "no puede ser negativo")
    BigDecimal kilometros,

    LocalDate fechaProgramadaEntrega,

    Boolean esAmbigua,

    /**
     * Indica que el operario corrigió manualmente el pin del mapa porque la
     * geocodificación era ambigua. El pedido igualmente queda en revisión.
     */
    Boolean pinAjustado,

    @Size(max = 300, message = "no puede superar los 300 caracteres")
    String observacionGeo,

    Integer idPedidoOrigen
) {
    /**
     * Constructor compatible con los tests y con el contrato canónico anterior.
     */
    public CrearPedidoRequest(
        Long operarioId,
        Long clienteId,
        String descripcion,
        BigDecimal pesoKg,
        BigDecimal montoEnvio,
        String calle,
        Integer altura,
        String pisoDepto,
        String localidad,
        BigDecimal latitud,
        BigDecimal longitud,
        LocalDate fechaProgramada,
        FiabilidadGeocodificacion fiabilidadGeocodificacion
    ) {
        this(
            operarioId,
            clienteId,
            descripcion,
            pesoKg,
            montoEnvio,
            calle,
            altura,
            pisoDepto,
            localidad,
            latitud,
            longitud,
            fechaProgramada,
            fiabilidadGeocodificacion,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
