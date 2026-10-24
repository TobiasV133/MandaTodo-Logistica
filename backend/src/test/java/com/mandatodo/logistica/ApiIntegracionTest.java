package com.mandatodo.logistica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandatodo.logistica.auditoria.AuditoriaRepository;
import com.mandatodo.logistica.cliente.Cliente;
import com.mandatodo.logistica.cliente.ClienteRepository;
import com.mandatodo.logistica.cliente.TipoCliente;
import com.mandatodo.logistica.operario.EstadoOperario;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.operario.RolOperario;
import com.mandatodo.logistica.pedido.Pedido;
import com.mandatodo.logistica.pedido.PedidoRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica por HTTP el contrato que consume la pantalla de Gestión de pedidos:
 * listado, cambio de estado con recargo y emisión/reimpresión del remito.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:api-integracion;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@Transactional
class ApiIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OperarioRepository operarioRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    private Pedido pedido;
    private Operario operario;

    @BeforeEach
    void setUp() {
        Cliente cliente = clienteRepository.save(new Cliente(
            "CUIT",
            "30-71234567-8",
            "Distribuidora del Sur S.A.",
            "11 4567-8901",
            "contacto@delsur.com.ar",
            TipoCliente.Empresa,
            true
        ));
        operario = operarioRepository.save(new Operario(
            "Laura Gómez",
            "laura@mandatodo.com",
            "hash",
            RolOperario.Admin,
            EstadoOperario.Activo
        ));
        pedido = pedidoRepository.save(new Pedido(
            cliente,
            "Caja de electrónica",
            new BigDecimal("10.00"),
            new BigDecimal("5000.00"),
            "Av. Corrientes",
            1234,
            "4 B",
            "Ciudad Autónoma de Buenos Aires",
            null,
            null,
            LocalDate.now().plusDays(2),
            LocalDateTime.now()
        ));
    }

    @Test
    void permiteElOrigenDelFrontendPorCors() throws Exception {
        mvc.perform(options("/api/pedidos")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                "http://localhost:5173"
            ));
    }

    @Test
    void listaPedidosConElContratoQueConsumeElFrontend() throws Exception {
        mvc.perform(get("/api/pedidos"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].idPedido").value(pedido.getId()))
            .andExpect(jsonPath("$[0].cliente").value("Distribuidora del Sur S.A."))
            .andExpect(jsonPath("$[0].tipoCliente").value("Empresa"))
            .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
            .andExpect(jsonPath("$[0].pesoKg").value(10.00))
            .andExpect(jsonPath("$[0].montoEnvio").value(5000.00))
            .andExpect(jsonPath("$[0].revisiones").isArray());
    }

    @Test
    void aplicaRecargoYReprogramacionAlMarcarFallido() throws Exception {
        mvc.perform(patch("/api/pedidos/{id}/estado", pedido.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "estado": "FALLIDO",
                      "motivoFalla": "Ausente",
                      "notaFalla": "El receptor no estaba",
                      "operarioId": %d
                    }
                    """.formatted(operario.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reprogramado").value(true))
            .andExpect(jsonPath("$.pedido.estado").value("FALLIDO"))
            .andExpect(jsonPath("$.pedido.motivoFalla").value("Ausente"))
            .andExpect(jsonPath("$.pedido.montoEnvio").value(7500.00))
            .andExpect(jsonPath("$.recargoAplicado").value(2500.00))
            .andExpect(jsonPath("$.fechaReprogramada").isNotEmpty());
    }

    @Test
    void emiteElPdfYExigeMotivoEnLaReimpresion() throws Exception {
        MvcResult primera = mvc.perform(post("/api/remitos/{id}/imprimir", pedido.getId())
                .accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "operarioId": %d, "kilometros": 45.00 }
                    """.formatted(operario.getId())))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("X-Remito-Reimpresion", "false"))
            .andReturn();

        assertThat(primera.getResponse().getContentAsByteArray())
            .hasSizeGreaterThan(1000)
            .startsWith("%PDF".getBytes(StandardCharsets.ISO_8859_1));

        // La segunda emisión sin motivo debe fallar y no generar un PDF.
        mvc.perform(post("/api/remitos/{id}/imprimir", pedido.getId())
                .accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "operarioId": %d, "kilometros": 45.00 }
                    """.formatted(operario.getId())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("motivo de reimpresión es obligatorio")
            ));

        mvc.perform(post("/api/remitos/{id}/imprimir", pedido.getId())
                .accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operarioId": %d,
                      "motivoReimpresion": "Extrav\u00edo del remito original",
                      "kilometros": 45.00
                    }
                    """.formatted(operario.getId())))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("X-Remito-Reimpresion", "true"));

        assertThat(auditoriaRepository.findAllByOrderByFechaHoraDesc()).hasSize(2);
    }
}
