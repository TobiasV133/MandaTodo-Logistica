package com.mandatodo.logistica.pedido;

import com.mandatodo.logistica.tarifa.TarifaConfigService;
import com.mandatodo.logistica.tarifa.TarifaValores;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Tarifario escalonado de MandaTodo.
 *
 * <ul>
 *   <li>Nivel 1: hasta el límite base de kg y km, tarifa base.</li>
 *   <li>Nivel 2: hasta el límite medio de kg y km, tarifa media.</li>
 *   <li>Nivel 3: supera el nivel medio y suma el recargo por km extra y por kg extra.</li>
 * </ul>
 *
 * <p>Los importes y los límites se leen de {@link TarifaConfigService} y se pueden editar
 * desde la pantalla de pedidos. Si no hay configuración guardada se usan
 * {@link TarifaValores#POR_DEFECTO}. Los valores que superan la capacidad máxima no se
 * tarifan: se lanza {@link IllegalArgumentException} con el mensaje de capacidad excedida.</p>
 */
@Service
public class TarifaService {

    public static final String MENSAJE_CAPACIDAD_EXCEDIDA =
        TarifaValores.POR_DEFECTO.mensajeCapacidad();

    private final TarifaConfigService tarifaConfigService;

    /**
     * Constructor sin dependencias para pruebas unitarias y usos aislados: opera con los
     * valores por defecto.
     */
    public TarifaService() {
        this.tarifaConfigService = null;
    }

    @Autowired
    public TarifaService(TarifaConfigService tarifaConfigService) {
        this.tarifaConfigService = tarifaConfigService;
    }

    /**
     * Calcula el precio total del envío según el tarifario vigente.
     *
     * @throws IllegalArgumentException si el peso o la distancia superan la capacidad máxima
     */
    public BigDecimal calcular(BigDecimal pesoKg, BigDecimal kilometros) {
        return calcular(pesoKg, kilometros, valoresVigentes());
    }

    public BigDecimal calcular(
        BigDecimal pesoKg,
        BigDecimal kilometros,
        TarifaValores tarifa
    ) {
        BigDecimal peso = valorNoNulo(pesoKg);
        BigDecimal distancia = valorNoNulo(kilometros);

        validarCapacidad(peso, distancia, tarifa);

        if (peso.compareTo(tarifa.limitePesoBase()) <= 0
            && distancia.compareTo(tarifa.limiteKmBase()) <= 0) {
            return tarifa.tarifaBase().setScale(2, RoundingMode.HALF_UP);
        }

        if (peso.compareTo(tarifa.limitePesoMedio()) <= 0
            && distancia.compareTo(tarifa.limiteKmMedio()) <= 0) {
            return tarifa.tarifaMedia().setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal kilometrosExtra = distancia.subtract(tarifa.limiteKmMedio())
            .max(BigDecimal.ZERO);
        BigDecimal pesoExtra = peso.subtract(tarifa.limitePesoMedio())
            .max(BigDecimal.ZERO);
        BigDecimal recargo = kilometrosExtra.multiply(tarifa.recargoPorKm())
            .add(pesoExtra.multiply(tarifa.recargoPorKg()));

        return tarifa.tarifaMedia().add(recargo).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Valida que el pedido no supere la capacidad logística configurada.
     *
     * @throws IllegalArgumentException si el peso o la distancia superan la capacidad máxima
     */
    public void validarCapacidad(BigDecimal pesoKg, BigDecimal kilometros) {
        validarCapacidad(pesoKg, kilometros, valoresVigentes());
    }

    public void validarCapacidad(
        BigDecimal pesoKg,
        BigDecimal kilometros,
        TarifaValores tarifa
    ) {
        BigDecimal peso = valorNoNulo(pesoKg);
        BigDecimal distancia = valorNoNulo(kilometros);

        if (peso.signum() < 0 || distancia.signum() < 0) {
            throw new IllegalArgumentException(
                "El peso y los kilómetros no pueden ser negativos"
            );
        }
        if (peso.compareTo(tarifa.maxPesoKg()) > 0
            || distancia.compareTo(tarifa.maxKilometros()) > 0) {
            throw new IllegalArgumentException(tarifa.mensajeCapacidad());
        }
    }

    private TarifaValores valoresVigentes() {
        return tarifaConfigService == null
            ? TarifaValores.POR_DEFECTO
            : tarifaConfigService.obtenerValores();
    }

    private BigDecimal valorNoNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
