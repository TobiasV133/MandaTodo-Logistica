package com.mandatodo.logistica.geo.dto;

public record NormalizacionResponse(
    Double latitud,
    Double longitud,
    String direccionNormalizada,
    boolean esAmbigua,
    String observacion
) {
}
