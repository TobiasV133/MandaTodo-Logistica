package com.mandatodo.logistica.revision.dto;

import com.mandatodo.logistica.pedido.MotivoFalla;
import com.mandatodo.logistica.revision.AccionRevision;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ResolverRevisionRequest(
    @NotNull(message = "es obligatoria")
    AccionRevision accion,

    // Opcional: en producción el operario se toma del JWT.
    Long operarioId,

    MotivoFalla motivoFalla,

    @Size(max = 500, message = "no puede superar los 500 caracteres")
    String notaFalla,

    @Size(max = 200, message = "no puede superar los 200 caracteres")
    String calle,

    @Min(value = 0, message = "no puede ser negativa")
    Integer altura,

    @Size(max = 50, message = "no puede superar los 50 caracteres")
    String pisoDepto,

    @Size(max = 150, message = "no puede superar los 150 caracteres")
    String localidad,

    @DecimalMin(value = "-90.0", message = "debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "debe estar entre -90 y 90")
    BigDecimal latitud,

    @DecimalMin(value = "-180.0", message = "debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "debe estar entre -180 y 180")
    BigDecimal longitud
) {
}
