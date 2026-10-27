package com.mandatodo.logistica.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandatodo.logistica.geo.dto.NormalizacionResponse;
import com.mandatodo.logistica.geo.dto.NormalizarRequest;
import com.mandatodo.logistica.shared.error.ErrorHttpException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeoServiceTest {

    private static final String RESULTADO_FUERA_DE_LOCALIDAD = """
        [{"lat":"-34.6292597","lon":"-58.6189185","display_name":"Banfield, Gaona, El Palomar, Partido de Morón, Buenos Aires, Argentina","type":"residential","addresstype":"residential","address":{"road":"Gaona","suburb":"El Palomar","state_district":"Partido de Morón","state":"Buenos Aires","country":"Argentina"}}]
        """;

    private HttpServer server;
    private final Deque<Respuesta> respuestas = new ArrayDeque<>();
    private final List<String> consultas = new CopyOnWriteArrayList<>();

    @BeforeEach
    void iniciarServidor() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/search", this::responder);
        server.start();
    }

    @AfterEach
    void detenerServidor() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void usaLaPrimeraConsultaConResultadoYDevuelvePrecisionDePuerta() {
        respuestas.add(new Respuesta(200, "[]"));
        respuestas.add(new Respuesta(200, """
            [{"lat":"-34.6039595","lon":"-58.3842172","display_name":"1234, Avenida Corrientes, CABA, Argentina","type":"house","addresstype":"place","address":{"house_number":"1234","road":"Avenida Corrientes","city":"Buenos Aires","state":"Ciudad Autónoma de Buenos Aires","country":"Argentina"}}]
            """));

        NormalizacionResponse respuesta = servicio().normalizar(
            new NormalizarRequest("Av. Corrientes", "1234", "CABA")
        );

        assertThat(respuesta.esAmbigua()).isFalse();
        assertThat(respuesta.direccionNormalizada()).contains("Avenida Corrientes");
        assertThat(consultas).hasSize(2);
        assertThat(consultas.get(0)).contains("street=");
        assertThat(consultas.get(1)).contains("q=");
    }

    @Test
    void caeALocalidadCuandoNoEncuentraCalleNiAltura() {
        respuestas.add(new Respuesta(200, "[]"));
        respuestas.add(new Respuesta(200, "[]"));
        respuestas.add(new Respuesta(200, "[]"));
        respuestas.add(new Respuesta(200, """
            [{"lat":"-34.543006","lon":"-58.711966","display_name":"San Miguel, Partido de San Miguel, Buenos Aires, Argentina","type":"administrative","addresstype":"city","address":{"city":"San Miguel","state":"Buenos Aires","country":"Argentina"}}]
            """));

        NormalizacionResponse respuesta = servicio().normalizar(
            new NormalizarRequest("Ruta 8", "45", "San Miguel")
        );

        assertThat(respuesta.esAmbigua()).isTrue();
        assertThat(respuesta.observacion()).contains("localidad");
        assertThat(consultas).hasSize(4);
    }

    @Test
    void descartaResultadosDeOtraLocalidadAunqueElNombreAparezcaEnLaDireccion() {
        for (int i = 0; i < 4; i++) {
            respuestas.add(new Respuesta(200, RESULTADO_FUERA_DE_LOCALIDAD));
        }
        respuestas.add(new Respuesta(200, """
            [{"lat":"-34.7437455","lon":"-58.3960811","display_name":"Banfield, Partido de Lomas de Zamora, Buenos Aires, Argentina","type":"administrative","addresstype":"city","address":{"city":"Banfield","state_district":"Partido de Lomas de Zamora","state":"Buenos Aires","country":"Argentina"}}]
            """));

        NormalizacionResponse respuesta = servicio().normalizar(
            new NormalizarRequest("Calle 1", "1234", "Banfield")
        );

        assertThat(respuesta.direccionNormalizada()).contains("Partido de Lomas de Zamora");
        assertThat(respuesta.esAmbigua()).isTrue();
        assertThat(respuesta.observacion()).contains("localidad");
        assertThat(consultas).hasSize(5);
    }

    @Test
    void marcaAmbiguaCuandoLaCalleEncontradaNoCoincideConLaIngresada() {
        respuestas.add(new Respuesta(200, """
            [{"lat":"-34.7335394","lon":"-58.4081721","display_name":"1234, Avenida Presb��tero Pedro Francisco de Uriarte, Banfield, Argentina","type":"house","addresstype":"place","address":{"house_number":"1234","road":"Avenida Presb��tero Pedro Francisco de Uriarte","city":"Banfield","state_district":"Partido de Lomas de Zamora","state":"Buenos Aires","country":"Argentina"}}]
            """));

        NormalizacionResponse respuesta = servicio().normalizar(
            new NormalizarRequest("Av. San Mart��n", "1234", "Banfield")
        );

        assertThat(respuesta.esAmbigua()).isTrue();
        assertThat(respuesta.observacion()).contains("no coincide exactamente");
        assertThat(respuesta.direccionNormalizada()).contains("Uriarte");
        assertThat(consultas).hasSize(1);
    }

    @Test
    void usaLaAlturaSinPisoDeptoCuandoLaConsultaExactaNoDevuelveResultados() {
        respuestas.add(new Respuesta(200, "[]"));
        respuestas.add(new Respuesta(200, "[]"));
        respuestas.add(new Respuesta(200, """
            [{"lat":"-34.6039595","lon":"-58.3842172","display_name":"1234, Avenida Corrientes, CABA, Argentina","type":"house","addresstype":"place","address":{"house_number":"1234","road":"Avenida Corrientes","city":"Buenos Aires","state":"Ciudad Autónoma de Buenos Aires","country":"Argentina"}}]
            """));

        NormalizacionResponse respuesta = servicio().normalizar(
            new NormalizarRequest("Av. Corrientes", "1234 4B", "CABA")
        );

        assertThat(respuesta.esAmbigua()).isFalse();
        assertThat(consultas).hasSize(3);
        assertThat(consultas.get(2)).contains("1234");
        assertThat(consultas.get(2)).doesNotContain("4B");
    }

    @Test
    void informaErrorCuandoNingunaConsultaDevuelveResultados() {
        assertThatThrownBy(() -> servicio().normalizar(
            new NormalizarRequest("Calle Inventada", "123", "San Miguel")
        ))
            .isInstanceOf(ErrorHttpException.class)
            .hasMessageContaining("No encontramos la dirección");
    }

    @Test
    void respetaElLimiteDeConsultasDeNominatim() {
        respuestas.add(new Respuesta(429, "{}"));

        assertThatThrownBy(() -> servicio().normalizar(
            new NormalizarRequest("Av. Corrientes", "1234", "CABA")
        ))
            .isInstanceOf(ErrorHttpException.class)
            .hasMessageContaining("demasiadas consultas");
    }

    private GeoService servicio() {
        return new GeoService(
            new ObjectMapper(),
            new NominatimRateLimiter(1000) {
                @Override
                public synchronized void esperarTurno() {
                    // Sin espera en tests.
                }
            },
            "http://localhost:" + server.getAddress().getPort() + "/search",
            "MandaTodo/1.0 (test@mandatodo.local)"
        );
    }

    private void responder(HttpExchange exchange) throws IOException {
        consultas.add(exchange.getRequestURI().getQuery());
        Respuesta respuesta = respuestas.isEmpty()
            ? new Respuesta(200, "[]")
            : respuestas.removeFirst();
        byte[] body = respuesta.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(respuesta.status(), body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private record Respuesta(int status, String body) {
    }
}
