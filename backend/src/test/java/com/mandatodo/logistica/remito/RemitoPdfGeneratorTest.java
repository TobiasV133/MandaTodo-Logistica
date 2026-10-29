package com.mandatodo.logistica.remito;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RemitoPdfGeneratorTest {

    private final RemitoPdfGenerator generador = new RemitoPdfGenerator();

    @Test
    void generaUnPdfA4ConDetalleKilosKilometrosEImporte() throws IOException {
        byte[] pdf = generador.generar(datos(false, null));

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(1000);

        PdfReader lector = new PdfReader(pdf);
        try {
            assertThat(lector.getNumberOfPages()).isEqualTo(1);
        } finally {
            lector.close();
        }

        String texto = extraerTexto(pdf);
        assertThat(texto)
            .contains("MandáTodo")
            .contains("R-00000042")
            .contains("Distribuidora del Sur S.A.")
            .contains("CUIT 30-71234567-8")
            .contains("Av. Corrientes 1234, 4 B")
            .contains("Caja de electrónica")
            .contains("10,00 kg")
            .contains("45,00 km")
            .contains("18.500,00")
            .contains("Total a cobrar");
    }

    @Test
    void muestraElSubtotalYElRecargoPorEntregaFallida() throws IOException {
        String texto = extraerTexto(generador.generar(datosConRecargo()));

        assertThat(texto)
            .contains("Subtotal")
            .contains("6.000,00")
            .contains("Recargo por entrega fallida")
            .contains("3.000,00")
            .contains("Total a cobrar")
            .contains("9.000,00");
    }

    @Test
    void incluyeElEspacioParaFirmaYAclaracionDelReceptor() throws IOException {
        String texto = extraerTexto(generador.generar(datos(false, null)));

        assertThat(texto)
            .contains("Firma del receptor")
            .contains("Aclaración y DNI")
            .contains("El receptor declara haber recibido la mercadería");
    }

    @Test
    void noMarcaLaReimpresionEnLaPrimeraImpresion() throws IOException {
        String texto = extraerTexto(generador.generar(datos(false, null)));

        assertThat(texto).doesNotContain("REIMPRESIÓN");
    }

    @Test
    void marcaLaReimpresionConElMotivoRegistrado() throws IOException {
        String texto = extraerTexto(generador.generar(datos(true, "Extravío del remito original")));

        assertThat(texto)
            .contains("REIMPRESIÓN")
            .contains("Motivo: Extravío del remito original");
    }

    @Test
    void informaCuandoNoSeCargaronKilometros() throws IOException {
        DatosRemito sinKilometros = new DatosRemito(
            "R-00000042",
            LocalDateTime.of(2026, 10, 23, 9, 15),
            LocalDate.of(2026, 10, 26),
            "Distribuidora del Sur S.A.",
            "CUIT 30-71234567-8",
            "11 4567-8901",
            "contacto@delsur.com.ar",
            "Av. Corrientes 1234, 4 B",
            "Ciudad Autónoma de Buenos Aires",
            "PENDIENTE",
            "Caja de electrónica",
            new BigDecimal("10.00"),
            null,
            new BigDecimal("18500.00"),
            new BigDecimal("18500.00"),
            BigDecimal.ZERO,
            false,
            null,
            "Laura Gómez",
            empresa()
        );

        String texto = extraerTexto(generador.generar(sinKilometros));

        assertThat(texto).contains("Sin informar");
    }

    private String extraerTexto(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(lector).getTextFromPage(1);
        } finally {
            lector.close();
        }
    }

    private DatosRemito datos(boolean reimpresion, String motivo) {
        return new DatosRemito(
            "R-00000042",
            LocalDateTime.of(2026, 10, 23, 9, 15),
            LocalDate.of(2026, 10, 26),
            "Distribuidora del Sur S.A.",
            "CUIT 30-71234567-8",
            "11 4567-8901",
            "contacto@delsur.com.ar",
            "Av. Corrientes 1234, 4 B",
            "Ciudad Autónoma de Buenos Aires",
            "PENDIENTE",
            "Caja de electrónica",
            new BigDecimal("10.00"),
            new BigDecimal("45.00"),
            new BigDecimal("18500.00"),
            new BigDecimal("18500.00"),
            BigDecimal.ZERO,
            reimpresion,
            motivo,
            "Laura Gómez",
            empresa()
        );
    }

    private DatosRemito datosConRecargo() {
        return new DatosRemito(
            "R-00000042",
            LocalDateTime.of(2026, 10, 23, 9, 15),
            LocalDate.of(2026, 10, 26),
            "Distribuidora del Sur S.A.",
            "CUIT 30-71234567-8",
            "11 4567-8901",
            "contacto@delsur.com.ar",
            "Av. Corrientes 1234, 4 B",
            "Ciudad Autónoma de Buenos Aires",
            "FALLIDO",
            "Caja de electrónica",
            new BigDecimal("10.00"),
            new BigDecimal("45.00"),
            new BigDecimal("9000.00"),
            new BigDecimal("6000.00"),
            new BigDecimal("3000.00"),
            false,
            null,
            "Laura Gómez",
            empresa()
        );
    }

    private DatosRemito.DatosEmpresa empresa() {
        return new DatosRemito.DatosEmpresa(
            "MandáTodo",
            "Logística y distribución",
            "Av. Corrientes 1234, CABA",
            "11 4567-8901",
            ""
        );
    }
}
