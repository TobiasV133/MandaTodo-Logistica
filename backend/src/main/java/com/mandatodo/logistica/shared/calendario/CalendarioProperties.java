package com.mandatodo.logistica.shared.calendario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mandatodo.calendario")
public class CalendarioProperties {

    private List<LocalDate> feriados = new ArrayList<>();

    public List<LocalDate> getFeriados() {
        return feriados;
    }

    public void setFeriados(List<LocalDate> feriados) {
        this.feriados = feriados == null ? new ArrayList<>() : feriados;
    }
}
