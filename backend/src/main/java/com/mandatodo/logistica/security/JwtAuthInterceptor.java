package com.mandatodo.logistica.security;

import tools.jackson.databind.ObjectMapper;
import com.mandatodo.logistica.shared.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ID_OPERARIO = "idOperarioAutenticado";
    public static final String ATTR_EMAIL = "emailAutenticado";
    public static final String ATTR_ROL = "rolAutenticado";
    public static final String ATTR_NOMBRE = "nombreAutenticado";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            escribirNoAutorizado(response, request, "Falta el token de autenticación.");
            return false;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        Optional<JwtService.JwtClaims> claims = jwtService.validar(token);
        if (claims.isEmpty()) {
            escribirNoAutorizado(response, request, "El token es inválido o expiró.");
            return false;
        }

        JwtService.JwtClaims datos = claims.get();
        request.setAttribute(ATTR_ID_OPERARIO, datos.idOperario());
        request.setAttribute(ATTR_EMAIL, datos.email());
        request.setAttribute(ATTR_ROL, datos.rol());
        request.setAttribute(ATTR_NOMBRE, datos.nombreCompleto());
        return true;
    }

    private void escribirNoAutorizado(
        HttpServletResponse response,
        HttpServletRequest request,
        String mensaje
    ) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse cuerpo = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            mensaje,
            request.getRequestURI()
        );
        objectMapper.writeValue(response.getWriter(), cuerpo);
    }
}
