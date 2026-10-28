package com.mandatodo.logistica.shared.calendario;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Calcula días hábiles administrativos: de lunes a viernes, excluyendo los
 * feriados configurados en {@code mandatodo.calendario.feriados}.
 */
@Component
public class CalendarioHabil {

    private final Set<LocalDate> feriados;

    @Autowired
    public CalendarioHabil(CalendarioProperties properties) {
        this(properties.getFeriados());
    }

    public CalendarioHabil(Collection<LocalDate> feriados) {
        this.feriados = feriados == null
            ? Set.of()
            : Set.copyOf(new HashSet<>(feriados));
    }

    public boolean esHabil(LocalDate fecha) {
        if (fecha == null) {
            return false;
        }
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY
            && dia != DayOfWeek.SUNDAY
            && !feriados.contains(fecha);
    }

    /**
     * Devuelve el primer día hábil posterior a {@code desde}. Si {@code desde}
     * es viernes, devuelve el lunes; si el lunes es feriado, devuelve el martes.
     */
    public LocalDate siguienteDiaHabil(LocalDate desde) {
        if (desde == null) {
            throw new IllegalArgumentException("La fecha de referencia es obligatoria");
        }
        LocalDate candidato = desde.plusDays(1);
        while (!esHabil(candidato)) {
            candidato = candidato.plusDays(1);
        }
        return candidato;
    }
}
