package com.mandatodo.logistica.config;

import com.mandatodo.logistica.pedido.EstadoPedido;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permite usar estados en minúsculas o con espacios en query params, por
 * ejemplo {@code GET /api/pedidos?estado=en_revision}.
 */
@Configuration
public class ConversionConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, EstadoPedido.class, EstadoPedido::desde);
    }
}
