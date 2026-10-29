package com.mandatodo.logistica.shared.calendario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CalendarioHabilTest {

    private final CalendarioHabil calendario = new CalendarioHabil(Set.of());

    @Test
    void unViernesSeReprogramaAlLunes() {
        assertThat(calendario.siguienteDiaHabil(LocalDate.of(2026, 10, 23)))
            .isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void unSabadoSeReprogramaAlLunes() {
        assertThat(calendario.siguienteDiaHabil(LocalDate.of(2026, 10, 24)))
            .isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void unDomingoSeReprogramaAlLunes() {
        assertThat(calendario.siguienteDiaHabil(LocalDate.of(2026, 10, 25)))
            .isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void unFeriadoSeSaltea() {
        CalendarioHabil conFeriado = new CalendarioHabil(Set.of(LocalDate.of(2026, 10, 26)));

        assertThat(conFeriado.siguienteDiaHabil(LocalDate.of(2026, 10, 23)))
            .isEqualTo(LocalDate.of(2026, 10, 27));
    }

    @Test
    void unFeriadoEnViernesSeSalteaJuntoConElFinDeSemana() {
        CalendarioHabil conFeriado = new CalendarioHabil(Set.of(LocalDate.of(2026, 10, 23)));

        assertThat(conFeriado.siguienteDiaHabil(LocalDate.of(2026, 10, 22)))
            .isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void identificaDiasHabilesYNoHabiles() {
        assertThat(calendario.esHabil(LocalDate.of(2026, 10, 23))).isTrue();
        assertThat(calendario.esHabil(LocalDate.of(2026, 10, 24))).isFalse();
        assertThat(calendario.esHabil(LocalDate.of(2026, 10, 25))).isFalse();

        CalendarioHabil conFeriado = new CalendarioHabil(Set.of(LocalDate.of(2026, 10, 26)));
        assertThat(conFeriado.esHabil(LocalDate.of(2026, 10, 26))).isFalse();
    }

    @Test
    void rechazaUnaFechaDeReferenciaNula() {
        assertThatThrownBy(() -> calendario.siguienteDiaHabil(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha de referencia");
    }
}
