package com.mandatodo.backend.service;

import com.mandatodo.backend.dto.auth.LoginRequest;
import com.mandatodo.backend.dto.auth.LoginResponse;
import com.mandatodo.backend.dto.auth.OperarioResponse;
import com.mandatodo.backend.exception.ApiException;
import com.mandatodo.backend.model.EstadoOperario;
import com.mandatodo.backend.model.Operario;
import com.mandatodo.backend.repository.OperarioRepository;
import com.mandatodo.backend.security.JwtService;
import com.mandatodo.backend.security.PasswordService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos.";

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
        String username = request.username() == null
                ? ""
                : request.username().trim().toLowerCase();

        Operario operario = operarioRepository.findByUsernameIgnoreCase(username)
                .or(() -> operarioRepository.findByEmailIgnoreCase(username))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario o contraseña incorrectos."
                ));

        if (operario.getEstado() != EstadoOperario.Activo
                || !passwordService.matches(request.password(), operario.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }

        String token = jwtService.generar(operario);
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                OperarioResponse.from(operario)
        );
    }

    @Transactional(readOnly = true)
    public OperarioResponse obtenerSesion(Integer idOperario) {
        Operario operario = operarioRepository.findById(idOperario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida."));

        if (operario.getEstado() != EstadoOperario.Activo) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "El usuario se encuentra inactivo.");
        }
        return OperarioResponse.from(operario);
    }
}
