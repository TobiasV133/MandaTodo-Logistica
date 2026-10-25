package com.mandatodo.backend.security;

import tools.jackson.databind.ObjectMapper;
import com.mandatodo.backend.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ID_OPERARIO = "idOperario";
    public static final String ATTR_USERNAME = "username";
    public static final String ATTR_ROL = "rol";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response, "Iniciá sesión para continuar.");
            return false;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        Optional<JwtService.JwtClaims> claims = jwtService.validar(token);
        if (claims.isEmpty()) {
            writeUnauthorized(response, "La sesión expiró. Volvé a iniciar sesión.");
            return false;
        }

        JwtService.JwtClaims data = claims.get();
        request.setAttribute(ATTR_ID_OPERARIO, data.idOperario());
        request.setAttribute(ATTR_USERNAME, data.username());
        request.setAttribute(ATTR_ROL, data.rol());
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError body = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                List.of()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
