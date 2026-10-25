package com.mandatodo.backend.util;

import com.mandatodo.backend.exception.ApiException;
import com.mandatodo.backend.model.TipoDocumento;
import org.springframework.http.HttpStatus;

public final class Documentos {

    private Documentos() {
    }

    /**
     * El DER guarda el número sin separadores. La búsqueda y la persistencia usan el mismo criterio.
     */
    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("[^0-9]", "");
    }

    public static String normalizarYValidar(String valor, TipoDocumento tipoDocumento) {
        String normalizado = normalizar(valor);
        if (tipoDocumento == TipoDocumento.DNI && (normalizado.length() < 7 || normalizado.length() > 8)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El DNI debe tener 7 u 8 dígitos.");
        }
        if (tipoDocumento == TipoDocumento.CUIT && normalizado.length() != 11) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El CUIT debe tener 11 dígitos.");
        }
        return normalizado;
    }
}
