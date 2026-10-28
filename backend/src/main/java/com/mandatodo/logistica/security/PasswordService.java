package com.mandatodo.logistica.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Los hashes de seeds.sql son BCrypt. Se usa el mismo algoritmo para validar
 * el login y para generar nuevos hashes si se incorporan usuarios.
 */
@Service
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public boolean matches(String passwordPlano, String hashGuardado) {
        return passwordPlano != null
            && hashGuardado != null
            && !hashGuardado.isBlank()
            && encoder.matches(passwordPlano, hashGuardado);
    }

    public String hash(String passwordPlano) {
        return encoder.encode(passwordPlano);
    }
}
