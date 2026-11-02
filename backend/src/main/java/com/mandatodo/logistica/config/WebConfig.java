package com.mandatodo.logistica.config;

import com.mandatodo.logistica.security.JwtAuthInterceptor;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final String[] allowedOrigins;
    private final boolean securityEnabled;

    public WebConfig(
        JwtAuthInterceptor jwtAuthInterceptor,
        @Value("${mandatodo.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
        String allowedOrigins,
        @Value("${mandatodo.security.enabled:true}") boolean securityEnabled
    ) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origen -> !origen.isEmpty())
            .toArray(String[]::new);
        this.securityEnabled = securityEnabled;
    }

    /**
     * CORS a nivel de filtro: se aplica también a errores que ocurren antes de
     * resolver el handler (por ejemplo un 405 por método no soportado), que de
     * otro modo llegarían al navegador sin las cabeceras y como error de red.
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuracion.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuracion.setAllowedHeaders(List.of("*"));
        configuracion.setExposedHeaders(List.of(
            "Content-Disposition",
            "X-Remito-Numero",
            "X-Remito-Reimpresion"
        ));

        UrlBasedCorsConfigurationSource origen = new UrlBasedCorsConfigurationSource();
        origen.registerCorsConfiguration("/api/**", configuracion);
        return new CorsFilter(origen);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!securityEnabled) {
            return;
        }
        registry.addInterceptor(jwtAuthInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/login");
    }
}
