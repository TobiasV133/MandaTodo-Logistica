package com.mandatodo.backend.dto.geo;

public record NormalizacionResponse(
        Double latitud,
        Double longitud,
        String direccionNormalizada,
        boolean esAmbigua,
        String observacion
) {
}
