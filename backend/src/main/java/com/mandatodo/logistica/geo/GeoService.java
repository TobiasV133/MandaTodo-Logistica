package com.mandatodo.logistica.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandatodo.logistica.geo.dto.NormalizacionResponse;
import com.mandatodo.logistica.geo.dto.NormalizarRequest;
import com.mandatodo.logistica.shared.error.ErrorHttpException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Geocodificación con Nominatim (OpenStreetMap). Sin SDK ni claves pagas.
 * Respeta el User-Agent identificable y el límite de 1 request por segundo.
 */
@Service
public class GeoService {

    private static final Logger log = LoggerFactory.getLogger(GeoService.class);

    private static final Set<String> TIPOS_PRECISOS = Set.of(
        "house", "building", "apartments", "residential", "commercial", "retail"
    );

    private static final Set<String> TIPOS_LOCALIDAD = Set.of(
        "administrative",
        "city",
        "town",
        "village",
        "municipality",
        "county",
        "state",
        "suburb",
        "neighbourhood"
    );

    private static final Pattern ALTURA_CON_DIGITOS = Pattern.compile("^(\\d{1,6})");

    private static final Map<String, Set<String>> ALIAS_LOCALIDAD = Map.of(
        "caba", Set.of("ciudad autonoma de buenos aires", "buenos aires"),
        "capital federal", Set.of("ciudad autonoma de buenos aires", "buenos aires"),
        "bs as", Set.of("buenos aires"),
        "gba", Set.of("buenos aires")
    );

    private static final List<String> PREFIJOS_CALLE = List.of(
        "avenida",
        "av",
        "calle",
        "pasaje",
        "pje",
        "boulevard",
        "bv",
        "diagonal",
        "diag",
        "camino",
        "ruta nacional",
        "ruta provincial",
        "ruta"
    );

    private enum Alcance {
        COMPLETA,
        CALLE,
        LOCALIDAD
    }

    private record Consulta(URI uri, Alcance alcance, boolean incluyeAltura) {
    }

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
        String calle = limpiar(request.calle());
        String localidad = limpiar(request.localidad());
        String altura = limpiar(request.altura());
        boolean alturaInvalida = esAlturaInvalida(altura);

        List<Consulta> consultas = construirConsultas(calle, altura, localidad, alturaInvalida);
        for (Consulta consulta : consultas) {
            rateLimiter.esperarTurno();

            HttpResponse<String> response = enviar(consulta.uri());
            validarRespuesta(response);
            JsonNode resultados = leerResultados(response.body());

            if (resultados.isEmpty()) {
                log.debug(
                    "Nominatim sin resultados para {} (alcance={})",
                    consulta.uri(),
                    consulta.alcance()
                );
                continue;
            }

            log.debug(
                "Nominatim devolvió {} resultado(s) para {} (alcance={})",
                resultados.size(),
                consulta.uri(),
                consulta.alcance()
            );

            JsonNode mejor = resultados.get(0);
            String direccionNormalizada = mejor.path("display_name").asText("");
            if (!coincideLocalidad(localidad, direccionNormalizada, mejor.path("address"))) {
                log.debug(
                    "Nominatim devolvió un resultado fuera de la localidad {} para {}; se descarta",
                    localidad,
                    consulta.uri()
                );
                continue;
            }

            return interpretarResultado(mejor, consulta, alturaInvalida, calle, localidad);
        }

        throw new ErrorHttpException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "No encontramos la dirección en OpenStreetMap. Probá con la calle sin el número, "
                + "agregá la provincia (por ejemplo, Buenos Aires) o revisá la localidad."
        );
    }

    private List<Consulta> construirConsultas(
        String calle,
        String altura,
        String localidad,
        boolean alturaInvalida
    ) {
        Map<String, Consulta> consultas = new LinkedHashMap<>();

        if (!alturaInvalida) {
            for (String varianteAltura : variantesAltura(altura)) {
                agregarConsulta(
                    consultas,
                    construirUriEstructurada(calle + " " + varianteAltura, localidad),
                    Alcance.COMPLETA,
                    true
                );
                agregarConsulta(
                    consultas,
                    construirUriLibre(construirConsulta(calle, varianteAltura, localidad)),
                    Alcance.COMPLETA,
                    true
                );
            }
        }

        agregarConsulta(
            consultas,
            construirUriEstructurada(calle, localidad),
            Alcance.CALLE,
            false
        );
        agregarConsulta(
            consultas,
            construirUriLibre(calle + ", " + localidad + ", Argentina"),
            Alcance.CALLE,
            false
        );
        agregarConsulta(
            consultas,
            construirUriEstructuradaLocalidad(localidad),
            Alcance.LOCALIDAD,
            false
        );
        agregarConsulta(
            consultas,
            construirUriLibre(localidad + ", Argentina"),
            Alcance.LOCALIDAD,
            false
        );

        return new ArrayList<>(consultas.values());
    }

    private List<String> variantesAltura(String altura) {
        List<String> variantes = new ArrayList<>();
        variantes.add(altura);

        Matcher matcher = ALTURA_CON_DIGITOS.matcher(altura);
        if (matcher.find()) {
            String digitos = matcher.group(1);
            if (!digitos.equals(altura)) {
                variantes.add(digitos);
            }
        }

        return variantes;
    }

    private void agregarConsulta(
        Map<String, Consulta> consultas,
        URI uri,
        Alcance alcance,
        boolean incluyeAltura
    ) {
        consultas.putIfAbsent(uri.toString(), new Consulta(uri, alcance, incluyeAltura));
    }

    private NormalizacionResponse interpretarResultado(
        JsonNode mejor,
        Consulta consulta,
        boolean alturaInvalida,
        String calle,
        String localidad
    ) {
        double latitud = mejor.path("lat").asDouble(Double.NaN);
        double longitud = mejor.path("lon").asDouble(Double.NaN);
        String direccionNormalizada = mejor.path("display_name").asText("");

        if (!Double.isFinite(latitud) || !Double.isFinite(longitud)) {
            throw new ErrorHttpException(
                HttpStatus.BAD_GATEWAY,
                "El servicio de geolocalización devolvió coordenadas inválidas."
            );
        }

        JsonNode address = mejor.path("address");
        String numeroPuerta = address.path("house_number").asText("");
        String tipo = mejor.path("type").asText("");
        String tipoDireccion = mejor.path("addresstype").asText("");

        boolean tieneNumero = !numeroPuerta.isBlank();
        boolean precisionBaja = !tieneNumero
            && !TIPOS_PRECISOS.contains(tipo)
            && !TIPOS_PRECISOS.contains(tipoDireccion);
        boolean faltaAlturaExacta = consulta.incluyeAltura() && !alturaInvalida && !tieneNumero;
        boolean localidadCoincide = coincideLocalidad(localidad, direccionNormalizada, address);
        boolean calleCoincide = coincideCalle(calle, address.path("road").asText(""));
        boolean resultadoLocalidad = !tieneNumero && (
            TIPOS_LOCALIDAD.contains(tipo)
                || TIPOS_LOCALIDAD.contains(tipoDireccion)
                || (
                    address.path("road").asText("").isBlank()
                        && !address.path("city").asText("").isBlank()
                )
        );

        boolean esAmbigua = alturaInvalida
            || !consulta.incluyeAltura()
            || consulta.alcance() == Alcance.LOCALIDAD
            || resultadoLocalidad
            || !calleCoincide
            || faltaAlturaExacta
            || precisionBaja
            || !localidadCoincide;

        String observacion = null;
        if (consulta.alcance() == Alcance.LOCALIDAD || resultadoLocalidad) {
            observacion = "No se encontró la calle exacta. Se geolocalizó la localidad; "
                + "revisá la dirección antes de despachar.";
        } else if (alturaInvalida) {
            observacion = "La altura está vacía, es S/N o 0. Se geolocalizó a nivel de calle "
                + "y requiere revisión.";
        } else if (!calleCoincide) {
            observacion = "La calle encontrada no coincide exactamente con la ingresada. "
                + "Revisá la dirección antes de despachar.";
        } else if (!consulta.incluyeAltura()) {
            observacion = "No se encontró la altura exacta. Se geolocalizó a nivel de calle; "
                + "revisá la dirección.";
        } else if (faltaAlturaExacta) {
            observacion = "Nominatim no encontró la altura exacta. Se geolocalizó una referencia aproximada.";
        } else if (precisionBaja) {
            observacion = "La precisión obtenida es de calle o localidad, no de puerta.";
        } else if (!localidadCoincide) {
            observacion = "La dirección encontrada no coincide exactamente con la localidad ingresada. "
                + "Revisá los datos.";
        }

        return new NormalizacionResponse(
            redondear(latitud),
            redondear(longitud),
            direccionNormalizada,
            esAmbigua,
            observacion
        );
    }

    private boolean coincideLocalidad(
        String localidad,
        String direccionNormalizada,
        JsonNode address
    ) {
        String localidadNormalizada = normalizarComparacion(localidad);
        if (localidadNormalizada.isBlank()) {
            return true;
        }

        String textoCampos = normalizarComparacion(construirTextoLocalidad(address));
        if (!textoCampos.isBlank()) {
            return contieneLocalidad(localidadNormalizada, textoCampos);
        }

        String textoCompleto = normalizarComparacion(direccionNormalizada);
        return contieneLocalidad(localidadNormalizada, textoCompleto);
    }

    private String construirTextoLocalidad(JsonNode address) {
        StringBuilder texto = new StringBuilder();
        for (String campo : List.of(
            "city",
            "town",
            "village",
            "municipality",
            "suburb",
            "county",
            "state_district"
        )) {
            texto.append(' ').append(address.path(campo).asText(""));
        }
        return texto.toString();
    }

    private boolean contieneLocalidad(String localidadNormalizada, String textoNormalizado) {
        if (textoNormalizado.contains(localidadNormalizada)) {
            return true;
        }

        Set<String> alias = ALIAS_LOCALIDAD.get(localidadNormalizada);
        if (alias != null) {
            for (String variante : alias) {
                if (textoNormalizado.contains(variante)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean coincideCalle(String calleEsperada, String calleEncontrada) {
        String esperada = normalizarCalle(calleEsperada);
        String encontrada = normalizarCalle(calleEncontrada);

        if (esperada.isBlank() || encontrada.isBlank()) {
            return true;
        }
        if (esperada.equals(encontrada)
            || esperada.contains(encontrada)
            || encontrada.contains(esperada)) {
            return true;
        }

        List<String> tokensEsperados = tokens(esperada);
        List<String> tokensEncontrados = tokens(encontrada);
        if (tokensEsperados.isEmpty() || tokensEncontrados.isEmpty()) {
            return true;
        }

        return tokensEncontrados.containsAll(tokensEsperados)
            || tokensEsperados.containsAll(tokensEncontrados);
    }

    private String normalizarCalle(String calle) {
        String normalizada = normalizarComparacion(calle);
        for (String prefijo : PREFIJOS_CALLE) {
            if (normalizada.startsWith(prefijo + " ")) {
                return normalizada.substring(prefijo.length() + 1).trim();
            }
        }
        return normalizada;
    }

    private List<String> tokens(String texto) {
        return List.of(texto.split("\\s+"));
    }

    private String normalizarComparacion(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", " ")
            .trim();
    }

    private HttpResponse<String> enviar(URI uri) {
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
            .GET()
            .timeout(Duration.ofSeconds(12))
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Accept-Language", "es-AR,es;q=0.9")
            .build();

        try {
            return httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ErrorHttpException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "La validación geográfica fue interrumpida."
            );
        } catch (Exception ex) {
            throw new ErrorHttpException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo consultar el servicio de geolocalización. Intentá nuevamente."
            );
        }
    }

    private void validarRespuesta(HttpResponse<String> response) {
        if (response.statusCode() == 429) {
            throw new ErrorHttpException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de geolocalización está recibiendo demasiadas consultas. Esperá unos segundos."
            );
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ErrorHttpException(
                HttpStatus.BAD_GATEWAY,
                "El servicio de geolocalización respondió con un error inesperado."
            );
        }
    }

    private JsonNode leerResultados(String body) {
        try {
            JsonNode resultados = objectMapper.readTree(body);
            if (!resultados.isArray()) {
                throw new ErrorHttpException(
                    HttpStatus.BAD_GATEWAY,
                    "El servicio de geolocalización devolvió una respuesta inválida."
                );
            }
            return resultados;
        } catch (ErrorHttpException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorHttpException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo interpretar la respuesta del servicio de geolocalización."
            );
        }
    }

    private String construirConsulta(String calle, String altura, String localidad) {
        return calle + " " + altura + ", " + localidad + ", Argentina";
    }

    private URI construirUriLibre(String consulta) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("q", consulta);
        return construirUri(parametros);
    }

    private URI construirUriEstructurada(String calle, String localidad) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("street", calle);
        parametros.put("city", localidad);
        parametros.put("country", "Argentina");
        return construirUri(parametros);
    }

    private URI construirUriEstructuradaLocalidad(String localidad) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("city", localidad);
        parametros.put("country", "Argentina");
        return construirUri(parametros);
    }

    private URI construirUri(Map<String, String> parametros) {
        String separador = nominatimUrl.contains("?") ? "&" : "?";
        StringBuilder url = new StringBuilder(nominatimUrl).append(separador);
        boolean primero = true;

        for (Map.Entry<String, String> parametro : parametros.entrySet()) {
            if (!primero) {
                url.append('&');
            }
            url.append(parametro.getKey())
                .append('=')
                .append(codificar(parametro.getValue()));
            primero = false;
        }

        url.append("&format=jsonv2")
            .append("&addressdetails=1")
            .append("&limit=5")
            .append("&countrycodes=ar");

        return URI.create(url.toString());
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
    }

    /**
     * Nominatim interpreta mejor los espacios como %20 que como + en el parámetro q.
     */
    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
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
