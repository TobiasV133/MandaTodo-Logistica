package com.mandatodo.backend.service;

import com.mandatodo.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Nominatim exige como máximo 1 request por segundo desde el backend.
 * El lock serializa las llamadas y garantiza el intervalo mínimo configurado.
 */
@Component
public class NominatimRateLimiter {

    private final long intervaloMinimoMs;
    private long ultimaLlamadaMs = 0L;

    public NominatimRateLimiter(@Value("${mandatodo.geo.min-interval-ms:1100}") long intervaloMinimoMs) {
        this.intervaloMinimoMs = Math.max(intervaloMinimoMs, 1000L);
    }

    public synchronized void esperarTurno() {
        long ahora = System.currentTimeMillis();
        long espera = intervaloMinimoMs - (ahora - ultimaLlamadaMs);

        if (espera > 0) {
            try {
                Thread.sleep(espera);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ApiException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "La consulta al servicio de geolocalización fue interrumpida."
                );
            }
        }
        ultimaLlamadaMs = System.currentTimeMillis();
    }
}
