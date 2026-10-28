package com.mandatodo.logistica.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${mandatodo.zona-horaria:America/Argentina/Buenos_Aires}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
