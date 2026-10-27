package com.mandatodo.logistica.remito;

/**
 * Resultado de una impresión de remito. El contenido es el PDF listo para descargar.
 */
public record RemitoGenerado(
    byte[] contenido,
    String nombreArchivo,
    String numero,
    boolean reimpresion,
    String motivoReimpresion
) {
}
