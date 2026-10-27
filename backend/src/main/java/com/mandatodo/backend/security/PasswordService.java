package com.mandatodo.backend.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Codificación de contraseñas con BCrypt.
 *
 * <p>BCrypt es el algoritmo oficial del proyecto y es compatible con los
 * hashes cargados por {@code seeds.sql}.</p>
 */
@Component
public class PasswordService {

    private static final int BCRYPT_MAX_BYTES = 72;
    private static final String ERROR_LONGITUD =
            "La contraseña no puede superar los 72 bytes (límite de BCrypt).";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new IllegalArgumentException(ERROR_LONGITUD);
        }
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        return encoder.matches(rawPassword, storedHash);
    }
}
