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
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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
import java.io.IOException;
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
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "mandatodo.security.enabled=false"
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
    void laRaizInformaLaUrlDelFrontendYLosRecursosInexistentesDan404() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("ok"))
            .andExpect(jsonPath("$.frontendUrl").value("http://localhost:5173"));

        mvc.perform(get("/no-existe"))
            .andExpect(status().isNotFound());
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
    void calculaLaTarifaEscalonadaAlCrearUnPedidoSinMonto() throws Exception {
        mvc.perform(post("/api/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operarioId": %d,
                      "clienteId": %d,
                      "descripcion": "Carga pesada",
                      "pesoKg": 21.00,
                      "kilometros": 16.00,
                      "calle": "Av. Corrientes",
                      "altura": 1234,
                      "localidad": "Ciudad Autónoma de Buenos Aires"
                    }
                    """.formatted(operario.getId(), pedido.getCliente().getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.pedido.montoEnvio").value(38000.00));
    }

    @Test
    void registraEnLaAuditoriaLaCorreccionManualDelPin() throws Exception {
        mvc.perform(post("/api/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operarioId": %d,
                      "clienteId": %d,
                      "descripcion": "Carga con pin corregido",
                      "pesoKg": 5.00,
                      "kilometros": 4.00,
                      "calle": "Av. Alsina",
                      "altura": 1234,
                      "localidad": "Banfield",
                      "esAmbigua": true,
                      "pinAjustado": true
                    }
                    """.formatted(operario.getId(), pedido.getCliente().getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.pedido.estado").value("EN_REVISION"));

        assertThat(auditoriaRepository.findAllByOrderByFechaHoraDesc())
            .anyMatch(evento -> evento.getActividad()
                .contains("ubicación del pin corregida manualmente"));
    }

    @Test
    void rechazaUnPedidoQueSuperaLaCapacidadLogistica() throws Exception {
        mvc.perform(post("/api/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operarioId": %d,
                      "clienteId": %d,
                      "descripcion": "Carga fuera de capacidad",
                      "pesoKg": 81.00,
                      "kilometros": 10.00,
                      "calle": "Av. Corrientes",
                      "altura": 1234,
                      "localidad": "Ciudad Autónoma de Buenos Aires"
                    }
                    """.formatted(operario.getId(), pedido.getCliente().getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "El pedido excede la capacidad logística máxima (80kg / 30km)"
            ));
    }

    @Test
    void rechazaLaCapacidadExcedidaAunqueSeEnvieUnMontoExplicito() throws Exception {
        mvc.perform(post("/api/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operarioId": %d,
                      "clienteId": %d,
                      "descripcion": "Carga con monto manual",
                      "pesoKg": 10.00,
                      "kilometros": 31.00,
                      "montoEnvio": 5000.00,
                      "calle": "Av. Corrientes",
                      "altura": 1234,
                      "localidad": "Ciudad Autónoma de Buenos Aires"
                    }
                    """.formatted(operario.getId(), pedido.getCliente().getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "El pedido excede la capacidad logística máxima (80kg / 30km)"
            ));
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
    void elRemitoSeparaElSubtotalYElRecargoDeUnaEntregaFallida() throws Exception {
        mvc.perform(patch("/api/pedidos/{id}/estado", pedido.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "estado": "FALLIDO",
                      "motivoFalla": "Ausente",
                      "operarioId": %d
                    }
                    """.formatted(operario.getId())))
            .andExpect(status().isOk());

        MvcResult remito = mvc.perform(post("/api/remitos/{id}/imprimir", pedido.getId())
                .accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "operarioId": %d, "kilometros": 45.00 }
                    """.formatted(operario.getId())))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn();

        String texto = extraerTexto(remito.getResponse().getContentAsByteArray());
        assertThat(texto)
            .contains("Subtotal")
            .contains("5.000,00")
            .contains("Recargo por entrega fallida")
            .contains("2.500,00")
            .contains("Total a cobrar")
            .contains("7.500,00");
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

    private String extraerTexto(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(lector).getTextFromPage(1);
        } finally {
            lector.close();
        }
    }
}
