package com.mandatodo.backend.util;

public final class Direcciones {

    private Direcciones() {
    }

    public static boolean esIncompleta(String calle, Integer altura, String localidad) {
        return esBlanco(calle)
                || esBlanco(localidad)
                || altura == null
                || altura <= 0
                || "s/n".equalsIgnoreCase(calle.trim())
                || "sin numero".equalsIgnoreCase(calle.trim());
    }

    public static String formatear(String calle, Integer altura, String pisoDepto, String localidad) {
        StringBuilder direccion = new StringBuilder();
        direccion.append(calle == null ? "" : calle.trim());
        direccion.append(altura != null && altura > 0 ? " " + altura : " S/N");

        if (pisoDepto != null && !pisoDepto.isBlank()) {
            direccion.append(", ").append(pisoDepto.trim());
        }
        direccion.append(", ").append(localidad == null ? "" : localidad.trim());
        return direccion.toString();
    }

    public static boolean esBlanco(String valor) {
        return valor == null || valor.isBlank();
    }

    public static String limpiarOpcional(String valor) {
        return esBlanco(valor) ? null : valor.trim();
    }
}
