package com.mandatodo.backend.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.mandatodo.backend.model.Operario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación mínima de JWT HS256 (RFC 7519) usando únicamente el JDK.
 * Evita agregar dependencias innecesarias al módulo de autenticación.
 */
@Service
public class JwtService {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${mandatodo.jwt.secret}") String secret,
            @Value("${mandatodo.jwt.expiration-seconds:28800}") long expirationSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String generar(Operario operario) {
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", operario.getUsername());
        payload.put("idOperario", operario.getIdOperario());
        payload.put("rol", operario.getRol().name());
        payload.put("nombreCompleto", operario.getNombreCompleto());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());

        try {
            String header = encode(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            String body = encode(objectMapper.writeValueAsBytes(payload));
            String unsigned = header + "." + body;
            String signature = encode(sign(unsigned));
            return unsigned + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el token de sesión", ex);
        }
    }

    public Optional<JwtClaims> validar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            String unsigned = parts[0] + "." + parts[1];
            byte[] expected = sign(unsigned);
            byte[] received = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expected, received)) {
                return Optional.empty();
            }

            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            long expiration = payload.path("exp").asLong(0);
            if (expiration <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            return Optional.of(new JwtClaims(
                    payload.path("idOperario").asInt(),
                    payload.path("sub").asText(),
                    payload.path("rol").asText(),
                    payload.path("nombreCompleto").asText()
            ));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private byte[] sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el token de sesión", ex);
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record JwtClaims(
            Integer idOperario,
            String username,
            String rol,
            String nombreCompleto
    ) {
    }
}
