package com.mandatodo.logistica.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mandatodo.logistica.tarifa.TarifaValores;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TarifaServiceTest {

    private final TarifaService tarifaService = new TarifaService();

    @Test
    void cobraLaTarifaBaseHastaCincoKilosYCincoKilometros() {
        assertThat(tarifaService.calcular(new BigDecimal("0.00"), new BigDecimal("0.00")))
            .isEqualByComparingTo("10000.00");
        assertThat(tarifaService.calcular(new BigDecimal("5.00"), new BigDecimal("5.00")))
            .isEqualByComparingTo("10000.00");
    }

    @Test
    void pasaAlNivelMedioSiCualquieraDeLasVariablesSuperaLimiteBase() {
        assertThat(tarifaService.calcular(new BigDecimal("5.01"), new BigDecimal("5.00")))
            .isEqualByComparingTo("30000.00");
        assertThat(tarifaService.calcular(new BigDecimal("5.00"), new BigDecimal("5.01")))
            .isEqualByComparingTo("30000.00");
        assertThat(tarifaService.calcular(new BigDecimal("20.00"), new BigDecimal("15.00")))
            .isEqualByComparingTo("30000.00");
    }

    @Test
    void aplicaElEjemploDeNivelTresConUnKilometroYUnKiloExtra() {
        assertThat(tarifaService.calcular(new BigDecimal("21.00"), new BigDecimal("16.00")))
            .isEqualByComparingTo("38000.00");
    }

    @Test
    void sumaLosRecargosDeKilometrosYKilosExtra() {
        assertThat(tarifaService.calcular(new BigDecimal("25.00"), new BigDecimal("20.00")))
            .isEqualByComparingTo("70000.00");
    }

    @Test
    void cobraLasFraccionesDeKilometroYKiloEnFormaProporcional() {
        assertThat(tarifaService.calcular(new BigDecimal("20.50"), new BigDecimal("15.50")))
            .isEqualByComparingTo("34000.00");
    }

    @Test
    void permiteExactamenteLaCapacidadMaxima() {
        assertThat(tarifaService.calcular(new BigDecimal("80.00"), new BigDecimal("30.00")))
            .isEqualByComparingTo("285000.00");
    }

    @Test
    void rechazaUnPesoMayorAochentaKilos() {
        assertThatThrownBy(() ->
            tarifaService.calcular(new BigDecimal("80.01"), new BigDecimal("10.00"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El pedido excede la capacidad logística máxima (80kg / 30km)");
    }

    @Test
    void rechazaUnaDistanciaMayorATreintaKilometros() {
        assertThatThrownBy(() ->
            tarifaService.calcular(new BigDecimal("10.00"), new BigDecimal("30.01"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El pedido excede la capacidad logística máxima (80kg / 30km)");
    }

    @Test
    void rechazaValoresNegativos() {
        assertThatThrownBy(() ->
            tarifaService.calcular(new BigDecimal("-1.00"), new BigDecimal("5.00"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pueden ser negativos");
    }

    @Test
    void aplicaImportesYUmbralesConfigurables() {
        TarifaValores personalizada = new TarifaValores(
            new BigDecimal("1000.00"),
            new BigDecimal("2000.00"),
            new BigDecimal("100.00"),
            new BigDecimal("50.00"),
            new BigDecimal("1.000"),
            new BigDecimal("1.000"),
            new BigDecimal("2.000"),
            new BigDecimal("2.000"),
            new BigDecimal("10.000"),
            new BigDecimal("10.000")
        );

        assertThat(tarifaService.calcular(
            new BigDecimal("1.00"),
            new BigDecimal("1.00"),
            personalizada
        )).isEqualByComparingTo("1000.00");

        assertThat(tarifaService.calcular(
            new BigDecimal("3.00"),
            new BigDecimal("4.00"),
            personalizada
        )).isEqualByComparingTo("2250.00");

        assertThatThrownBy(() ->
            tarifaService.calcular(
                new BigDecimal("11.00"),
                new BigDecimal("4.00"),
                personalizada
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El pedido excede la capacidad logística máxima (10kg / 10km)");
    }
}
