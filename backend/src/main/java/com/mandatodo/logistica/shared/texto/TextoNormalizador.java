package com.mandatodo.logistica.shared.texto;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalización de texto usada por el motor de retención y por las comparaciones
 * de direcciones. No modifica los datos persistidos.
 */
public final class TextoNormalizador {

    private TextoNormalizador() {
    }

    /**
     * Pasa a minúsculas, quita acentos y colapsa espacios. Conserva signos como
     * "/" para poder detectar calles "S/N".
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Normalización agresiva para comparar datos de dos pedidos: además de
     * minúsculas y acentos, reemplaza cualquier signo por un espacio.
     */
    public static String normalizarParaComparar(String texto) {
        return normalizar(texto)
            .replaceAll("[^a-z0-9]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Detecta las variantes habituales de una calle sin número de puerta.
     */
    public static boolean esCalleSinNumero(String calle) {
        String normalizada = normalizar(calle)
            .replace(".", "")
            .replace("°", "")
            .trim();
        return normalizada.equals("s/n")
            || normalizada.equals("sn")
            || normalizada.equals("sin numero")
            || normalizada.equals("sin nro")
            || normalizada.startsWith("s/n ")
            || normalizada.startsWith("sin numero ");
    }
}
