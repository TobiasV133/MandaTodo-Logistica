package com.mandatodo.logistica.shared;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Información mínima para quien abra la raíz del backend por error.
 * La interfaz se sirve desde Vite en el puerto 5173; este endpoint no forma
 * parte del contrato funcional de /api.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ApiInfoResponse inicio() {
        return new ApiInfoResponse(
            "MandáTodo Logística API",
            "ok",
            "http://localhost:5173",
            "El frontend no se sirve desde este puerto."
        );
    }

    public record ApiInfoResponse(
        String servicio,
        String estado,
        String frontendUrl,
        String mensaje
    ) {
    }
}
