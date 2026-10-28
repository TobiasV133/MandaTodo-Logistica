package com.mandatodo.logistica.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Error controlado que necesita un código HTTP distinto de 404/409.
 * Se usa, por ejemplo, para credenciales inválidas (401) y fallas de
 * geocodificación (422/502/503).
 */
public class ErrorHttpException extends RuntimeException {

    private final HttpStatus estado;

    public ErrorHttpException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
