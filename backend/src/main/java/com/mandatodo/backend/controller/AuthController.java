package com.mandatodo.backend.controller;

import com.mandatodo.backend.dto.auth.LoginRequest;
import com.mandatodo.backend.dto.auth.LoginResponse;
import com.mandatodo.backend.dto.auth.OperarioResponse;
import com.mandatodo.backend.security.JwtAuthInterceptor;
import com.mandatodo.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<OperarioResponse> obtenerSesion(
            @RequestAttribute(JwtAuthInterceptor.ATTR_ID_OPERARIO) Integer idOperario
    ) {
        return ResponseEntity.ok(authService.obtenerSesion(idOperario));
    }
}
