package com.mandatodo.logistica.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mandatodo.logistica.operario.Operario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JWT HS256 mínimo, sin dependencias externas. El token identifica al
 * operario autenticado y viaja en el header Authorization: Bearer.
 */
@Service
public class JwtService {

    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

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
        long ahora = Instant.now().getEpochSecond();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("sub", operario.getEmail());
        payload.put("idOperario", operario.getId());
        payload.put("email", operario.getEmail());
        payload.put("rol", operario.getRol().name());
        payload.put("nombreCompleto", operario.getNombre());
        payload.put("iat", ahora);
        payload.put("exp", ahora + expirationSeconds);

        String header = BASE64_URL.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)
        );
        String cuerpo = BASE64_URL.encodeToString(escribir(payload));
        String firma = BASE64_URL.encodeToString(firmar(header + "." + cuerpo));
        return header + "." + cuerpo + "." + firma;
    }

    public Optional<JwtClaims> validar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] partes = token.split("\\.");
        if (partes.length != 3) {
            return Optional.empty();
        }

        try {
            byte[] firmaEsperada = firmar(partes[0] + "." + partes[1]);
            byte[] firmaRecibida = BASE64_URL_DECODER.decode(partes[2]);
            if (!MessageDigest.isEqual(firmaEsperada, firmaRecibida)) {
                return Optional.empty();
            }

            JsonNode payload = objectMapper.readTree(BASE64_URL_DECODER.decode(partes[1]));
            long exp = payload.path("exp").asLong(0);
            if (exp <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            long idOperario = payload.path("idOperario").asLong(0);
            String email = payload.path("email").asText(null);
            if (idOperario <= 0 || email == null || email.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new JwtClaims(
                idOperario,
                email,
                payload.path("rol").asText(null),
                payload.path("nombreCompleto").asText(null)
            ));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private byte[] escribir(ObjectNode payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar el JWT", ex);
        }
    }

    private byte[] firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el JWT", ex);
        }
    }

    public record JwtClaims(
        Long idOperario,
        String email,
        String rol,
        String nombreCompleto
    ) {
    }
}
