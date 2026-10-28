package com.mandatodo.logistica.auth;

import com.mandatodo.logistica.auth.dto.LoginRequest;
import com.mandatodo.logistica.auth.dto.LoginResponse;
import com.mandatodo.logistica.auth.dto.OperarioResponse;
import com.mandatodo.logistica.operario.EstadoOperario;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.security.JwtService;
import com.mandatodo.logistica.security.PasswordService;
import com.mandatodo.logistica.shared.error.ErrorHttpException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Email o contraseña incorrectos.";

    private final OperarioRepository operarioRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    public AuthService(
        OperarioRepository operarioRepository,
        PasswordService passwordService,
        JwtService jwtService
    ) {
        this.operarioRepository = operarioRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.username().trim();
        Operario operario = operarioRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ErrorHttpException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS));

        if (operario.getEstado() != EstadoOperario.Activo
            || !passwordService.matches(request.password(), operario.getPasswordHash())) {
            throw new ErrorHttpException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }

        String token = jwtService.generar(operario);
        return new LoginResponse(
            token,
            "Bearer",
            jwtService.getExpirationSeconds(),
            OperarioResponse.desde(operario)
        );
    }

    @Transactional(readOnly = true)
    public OperarioResponse obtenerSesion(Long idOperario) {
        Operario operario = operarioRepository.findById(idOperario)
            .orElseThrow(() -> new ErrorHttpException(
                HttpStatus.UNAUTHORIZED,
                "La sesión ya no es válida."
            ));
        if (operario.getEstado() != EstadoOperario.Activo) {
            throw new ErrorHttpException(HttpStatus.UNAUTHORIZED, "El operario está inactivo.");
        }
        return OperarioResponse.desde(operario);
    }
}
