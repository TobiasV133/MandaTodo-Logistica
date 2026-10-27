package com.mandatodo.logistica.config;

import com.mandatodo.logistica.security.JwtAuthInterceptor;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders(
                "Content-Disposition",
                "X-Remito-Numero",
                "X-Remito-Reimpresion"
            );
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
