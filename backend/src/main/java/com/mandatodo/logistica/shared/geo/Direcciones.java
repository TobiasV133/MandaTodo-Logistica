package com.mandatodo.logistica.shared.geo;

import org.springframework.util.StringUtils;

public final class Direcciones {

    private Direcciones() {
    }

    public static String formatear(
        String calle,
        Integer altura,
        String pisoDepto,
        String localidad
    ) {
        StringBuilder direccion = new StringBuilder();
        if (StringUtils.hasText(calle)) {
            direccion.append(calle.trim());
        }

        if (altura != null && altura > 0) {
            if (!direccion.isEmpty()) {
                direccion.append(' ');
            }
            direccion.append(altura);
        }

        if (StringUtils.hasText(pisoDepto)) {
            if (!direccion.isEmpty()) {
                direccion.append(", ");
            }
            direccion.append(pisoDepto.trim());
        }

        if (StringUtils.hasText(localidad)) {
            if (!direccion.isEmpty()) {
                direccion.append(", ");
            }
            direccion.append(localidad.trim());
        }

        return direccion.toString();
    }

    public static boolean esIncompleta(
        String calle,
        Integer altura,
        String localidad
    ) {
        if (!StringUtils.hasText(calle) || !StringUtils.hasText(localidad)) {
            return true;
        }
        String calleNormalizada = calle.trim().toLowerCase();
        if (calleNormalizada.equals("s/n")
            || calleNormalizada.equals("sn")
            || calleNormalizada.equals("sin numero")
            || calleNormalizada.equals("sin número")) {
            return true;
        }
        return altura == null || altura <= 0;
    }
}
