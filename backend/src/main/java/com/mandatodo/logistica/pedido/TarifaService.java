package com.mandatodo.logistica.pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Tarifario escalonado de MandaTodo.
 *
 * <ul>
 *   <li>Nivel 1: hasta 5 kg y hasta 5 km, $10.000.</li>
 *   <li>Nivel 2: supera 5 kg o 5 km, sin superar 20 kg ni 15 km, $30.000.</li>
 *   <li>Nivel 3: más de 20 kg o más de 15 km, $30.000 más $5.000 por km extra y
 *       $3.000 por kg extra.</li>
 * </ul>
 *
 * <p>La capacidad máxima es de 80 kg y 30 km. Los valores que superen esos límites no se
 * tarifan: se lanza {@link IllegalArgumentException} con el mensaje de capacidad excedida.</p>
 */
@Service
public class TarifaService {

    public static final BigDecimal TARIFA_BASE = new BigDecimal("10000.00");
    public static final BigDecimal TARIFA_MEDIA = new BigDecimal("30000.00");
    public static final BigDecimal RECARGO_POR_KM = new BigDecimal("5000.00");
    public static final BigDecimal RECARGO_POR_KG = new BigDecimal("3000.00");
    public static final BigDecimal MAX_PESO_KG = new BigDecimal("80.00");
    public static final BigDecimal MAX_KILOMETROS = new BigDecimal("30.00");
    public static final String MENSAJE_CAPACIDAD_EXCEDIDA =
        "El pedido excede la capacidad logística máxima (80kg / 30km)";

    private static final BigDecimal LIMITE_PESO_BASE = new BigDecimal("5.00");
    private static final BigDecimal LIMITE_KM_BASE = new BigDecimal("5.00");
    private static final BigDecimal LIMITE_PESO_MEDIO = new BigDecimal("20.00");
    private static final BigDecimal LIMITE_KM_MEDIO = new BigDecimal("15.00");

    /**
     * Calcula el precio total del envío según el tarifario escalonado.
     *
     * @throws IllegalArgumentException si el peso o la distancia superan la capacidad máxima
     */
    public BigDecimal calcular(BigDecimal pesoKg, BigDecimal kilometros) {
        BigDecimal peso = valorNoNulo(pesoKg);
        BigDecimal distancia = valorNoNulo(kilometros);

        validarCapacidad(peso, distancia);

        if (peso.compareTo(LIMITE_PESO_BASE) <= 0
            && distancia.compareTo(LIMITE_KM_BASE) <= 0) {
            return TARIFA_BASE;
        }

        if (peso.compareTo(LIMITE_PESO_MEDIO) <= 0
            && distancia.compareTo(LIMITE_KM_MEDIO) <= 0) {
            return TARIFA_MEDIA;
        }

        BigDecimal kilometrosExtra = distancia.subtract(LIMITE_KM_MEDIO).max(BigDecimal.ZERO);
        BigDecimal pesoExtra = peso.subtract(LIMITE_PESO_MEDIO).max(BigDecimal.ZERO);
        BigDecimal recargo = kilometrosExtra.multiply(RECARGO_POR_KM)
            .add(pesoExtra.multiply(RECARGO_POR_KG));

        return TARIFA_MEDIA.add(recargo).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Valida que el pedido no supere la capacidad logística configurada.
     *
     * @throws IllegalArgumentException si el peso o la distancia superan la capacidad máxima
     */
    public void validarCapacidad(BigDecimal pesoKg, BigDecimal kilometros) {
        BigDecimal peso = valorNoNulo(pesoKg);
        BigDecimal distancia = valorNoNulo(kilometros);

        if (peso.signum() < 0 || distancia.signum() < 0) {
            throw new IllegalArgumentException(
                "El peso y los kilómetros no pueden ser negativos"
            );
        }
        if (peso.compareTo(MAX_PESO_KG) > 0 || distancia.compareTo(MAX_KILOMETROS) > 0) {
            throw new IllegalArgumentException(MENSAJE_CAPACIDAD_EXCEDIDA);
        }
    }

    private BigDecimal valorNoNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
