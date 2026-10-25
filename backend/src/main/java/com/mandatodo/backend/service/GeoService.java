package com.mandatodo.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.mandatodo.backend.dto.geo.NormalizacionResponse;
import com.mandatodo.backend.dto.geo.NormalizarRequest;
import com.mandatodo.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * Geocodificación con Nominatim (OpenStreetMap). Sin SDK ni claves pagas.
 * Respeta el User-Agent identificable y el límite de 1 request por segundo.
 */
@Service
public class GeoService {

    private static final Set<String> TIPOS_PRECISOS = Set.of(
            "house", "building", "apartments", "residential", "commercial", "retail"
    );

    private final ObjectMapper objectMapper;
    private final NominatimRateLimiter rateLimiter;
    private final HttpClient httpClient;
    private final String nominatimUrl;
    private final String userAgent;

    public GeoService(
            ObjectMapper objectMapper,
            NominatimRateLimiter rateLimiter,
            @Value("${mandatodo.geo.nominatim-url}") String nominatimUrl,
            @Value("${mandatodo.geo.user-agent}") String userAgent
    ) {
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
        this.nominatimUrl = nominatimUrl;
        this.userAgent = userAgent;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public NormalizacionResponse normalizar(NormalizarRequest request) {
        String calle = request.calle().trim();
        String localidad = request.localidad().trim();
        String altura = request.altura() == null ? "" : request.altura().trim();
        boolean alturaInvalida = esAlturaInvalida(altura);

        String consulta = construirConsulta(calle, altura, localidad, alturaInvalida);
        URI uri = construirUri(consulta);

        rateLimiter.esperarTurno();

        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .header("Accept-Language", "es-AR,es;q=0.9")
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "La validación geográfica fue interrumpida.");
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar el servicio de geolocalización. Intentá nuevamente."
            );
        }

        if (response.statusCode() == 429) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de geolocalización está recibiendo demasiadas consultas. Esperá unos segundos."
            );
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "El servicio de geolocalización respondió con un error inesperado."
            );
        }

        return interpretarRespuesta(response.body(), alturaInvalida);
    }

    private NormalizacionResponse interpretarRespuesta(String body, boolean alturaInvalida) {
        try {
            JsonNode resultados = objectMapper.readTree(body);
            if (!resultados.isArray() || resultados.isEmpty()) {
                throw new ApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "No se pudo geolocalizar la dirección. Revisá calle, altura y localidad."
                );
            }

            JsonNode mejor = resultados.get(0);
            double latitud = mejor.path("lat").asDouble(Double.NaN);
            double longitud = mejor.path("lon").asDouble(Double.NaN);
            String direccionNormalizada = mejor.path("display_name").asText("");

            if (!Double.isFinite(latitud) || !Double.isFinite(longitud)) {
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "El servicio de geolocalización devolvió coordenadas inválidas."
                );
            }

            JsonNode address = mejor.path("address");
            String numeroPuerta = address.path("house_number").asText("");
            String tipo = mejor.path("type").asText("");
            String tipoDireccion = mejor.path("addresstype").asText("");

            boolean faltaAlturaExacta = !alturaInvalida && numeroPuerta.isBlank();
            boolean precisionBaja = !TIPOS_PRECISOS.contains(tipo) && !TIPOS_PRECISOS.contains(tipoDireccion);
            boolean esAmbigua = alturaInvalida || faltaAlturaExacta || precisionBaja;

            String observacion = null;
            if (alturaInvalida) {
                observacion = "La altura está vacía, es S/N o 0. Se geolocalizó a nivel de calle y requiere revisión.";
            } else if (faltaAlturaExacta) {
                observacion = "Nominatim no encontró la altura exacta. Se geolocalizó una referencia aproximada.";
            } else if (precisionBaja) {
                observacion = "La precisión obtenida es de calle o localidad, no de puerta.";
            }

            return new NormalizacionResponse(
                    redondear(latitud),
                    redondear(longitud),
                    direccionNormalizada,
                    esAmbigua,
                    observacion
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo interpretar la respuesta del servicio de geolocalización."
            );
        }
    }

    private String construirConsulta(String calle, String altura, String localidad, boolean alturaInvalida) {
        StringBuilder consulta = new StringBuilder(calle);
        if (!alturaInvalida) {
            consulta.append(' ').append(altura);
        }
        consulta.append(", ").append(localidad).append(", Argentina");
        return consulta.toString();
    }

    private URI construirUri(String consulta) {
        String separador = nominatimUrl.contains("?") ? "&" : "?";
        String url = nominatimUrl + separador
                + "q=" + URLEncoder.encode(consulta, StandardCharsets.UTF_8)
                + "&format=jsonv2"
                + "&addressdetails=1"
                + "&limit=5"
                + "&countrycodes=ar";
        return URI.create(url);
    }

    private boolean esAlturaInvalida(String altura) {
        return altura.isBlank()
                || "0".equals(altura)
                || "s/n".equalsIgnoreCase(altura)
                || "sn".equalsIgnoreCase(altura)
                || "sin numero".equalsIgnoreCase(altura);
    }

    private Double redondear(double valor) {
        return BigDecimal.valueOf(valor)
                .setScale(7, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
