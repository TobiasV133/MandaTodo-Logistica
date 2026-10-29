package com.mandatodo.logistica.remito;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Genera el remito imprimible en formato A4 con OpenPDF. El documento incluye los datos del emisor
 * y del cliente, el detalle de la carga con kilos y kilómetros, el importe y el espacio de firma y
 * aclaración del receptor.
 *
 * <p>La reimpresión se marca con una banda roja que repite el motivo registrado en la auditoría.</p>
 */
@Component
public class RemitoPdfGenerator {

    private static final Locale LOCALE_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color AZUL_NOCHE = new Color(15, 23, 42);
    private static final Color GRIS = new Color(100, 116, 139);
    private static final Color GRIS_CLARO = new Color(248, 249, 250);
    private static final Color BORDE = new Color(203, 213, 225);
    private static final Color ROJO = new Color(185, 28, 28);
    private static final Color FONDO_ROJO = new Color(254, 226, 226);

    private static final Font MARCA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, AZUL_NOCHE);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS);
    private static final Font NUMERO = FontFactory.getFont(FontFactory.COURIER_BOLD, 16, AZUL_NOCHE);
    private static final Font ETIQUETA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, GRIS);
    private static final Font VALOR = FontFactory.getFont(FontFactory.HELVETICA, 9, AZUL_NOCHE);
    private static final Font VALOR_FUERTE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, AZUL_NOCHE);
    private static final Font TITULO_SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, AZUL_NOCHE);
    private static final Font ENCABEZADO_TABLA = FontFactory.getFont(
        FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE
    );
    private static final Font TEXTO_TABLA = FontFactory.getFont(FontFactory.HELVETICA, 9, AZUL_NOCHE);
    private static final Font TEXTO_TABLA_FUERTE = FontFactory.getFont(
        FontFactory.HELVETICA_BOLD, 9, AZUL_NOCHE
    );
    private static final Font CHICO = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);
    private static final Font PIE = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, GRIS);
    private static final Font BANNER_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, ROJO);
    private static final Font BANNER_TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, ROJO);

    public byte[] generar(DatosRemito datos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 45, 45, 40, 58);
        PdfWriter escritor = PdfWriter.getInstance(documento, salida);
        escritor.setPageEvent(new PieDePagina(datos));

        documento.open();
        documento.add(encabezado(datos));
        documento.add(espacio(12));

        if (datos.reimpresion()) {
            documento.add(bandaReimpresion(datos));
            documento.add(espacio(12));
        }

        documento.add(bloquesClienteYEntrega(datos));
        documento.add(espacio(18));

        documento.add(tituloSeccion("Detalle de la carga"));
        documento.add(espacio(6));
        documento.add(tablaDetalle(datos));
        documento.add(espacio(8));
        documento.add(tablaTotales(datos));
        documento.add(espacio(20));

        documento.add(tituloSeccion("Observaciones"));
        documento.add(espacio(6));
        documento.add(cajaObservaciones());
        documento.add(espacio(18));

        documento.add(tablaFirmas());
        documento.add(espacio(10));
        documento.add(new Paragraph("Fecha y hora de entrega: ______________________", VALOR));
        documento.add(espacio(10));
        documento.add(new Paragraph(
            "El receptor declara haber recibido la mercadería detallada en este remito.",
            CHICO
        ));

        documento.close();
        return salida.toByteArray();
    }

    private PdfPTable encabezado(DatosRemito datos) {
        PdfPTable tabla = new PdfPTable(new float[] { 62f, 38f });
        tabla.setWidthPercentage(100);

        DatosRemito.DatosEmpresa empresa = datos.empresa();
        PdfPCell izquierda = sinBorde();
        izquierda.addElement(new Paragraph(empresa.nombre(), MARCA));
        izquierda.addElement(new Paragraph(empresa.subtitulo(), SUBTITULO));
        if (StringUtils.hasText(empresa.direccion())) {
            izquierda.addElement(new Paragraph(empresa.direccion(), CHICO));
        }
        if (StringUtils.hasText(empresa.telefono())) {
            izquierda.addElement(new Paragraph("Tel. " + empresa.telefono(), CHICO));
        }
        if (StringUtils.hasText(empresa.cuit())) {
            izquierda.addElement(new Paragraph("CUIT " + empresa.cuit(), CHICO));
        }
        tabla.addCell(izquierda);

        PdfPCell derecha = sinBorde();
        derecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        derecha.addElement(parrafoDerecha("REMITO", ETIQUETA));
        derecha.addElement(parrafoDerecha(datos.numero(), NUMERO));
        derecha.addElement(parrafoDerecha(
            "Emitido: " + datos.fechaEmision().format(FECHA_HORA),
            CHICO
        ));
        derecha.addElement(parrafoDerecha(
            "Entrega programada: " + fecha(datos.fechaProgramada()),
            CHICO
        ));
        tabla.addCell(derecha);

        return tabla;
    }

    private PdfPTable bandaReimpresion(DatosRemito datos) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);

        PdfPCell celda = new PdfPCell();
        celda.setBackgroundColor(FONDO_ROJO);
        celda.setBorderColor(ROJO);
        celda.setBorderWidth(0.8f);
        celda.setPadding(8);
        celda.addElement(new Paragraph("REIMPRESIÓN", BANNER_TITULO));
        celda.addElement(new Paragraph(
            "Motivo: " + datos.motivoReimpresion()
                + ". Este ejemplar queda registrado en la auditoría del sistema.",
            BANNER_TEXTO
        ));
        tabla.addCell(celda);
        return tabla;
    }

    private PdfPTable bloquesClienteYEntrega(DatosRemito datos) {
        PdfPTable tabla = new PdfPTable(new float[] { 48f, 4f, 48f });
        tabla.setWidthPercentage(100);

        PdfPCell cliente = bloque("CLIENTE");
        cliente.addElement(new Paragraph(datos.clienteNombre(), VALOR_FUERTE));
        cliente.addElement(new Paragraph(datos.clienteDocumento(), VALOR));
        cliente.addElement(new Paragraph("Tel. " + datos.clienteTelefono(), VALOR));
        cliente.addElement(new Paragraph(datos.clienteEmail(), VALOR));
        tabla.addCell(cliente);

        tabla.addCell(celdaVacia());

        PdfPCell entrega = bloque("ENTREGA");
        entrega.addElement(new Paragraph(datos.direccion(), VALOR_FUERTE));
        entrega.addElement(new Paragraph(datos.localidad(), VALOR));
        entrega.addElement(new Paragraph("Estado del pedido: " + estado(datos.estado()), VALOR));
        entrega.addElement(new Paragraph("Fecha programada: " + fecha(datos.fechaProgramada()), VALOR));
        tabla.addCell(entrega);

        return tabla;
    }

    private PdfPTable tablaDetalle(DatosRemito datos) {
        PdfPTable tabla = new PdfPTable(new float[] { 52f, 14f, 14f, 20f });
        tabla.setWidthPercentage(100);

        tabla.addCell(celdaEncabezado("Descripción", Element.ALIGN_LEFT));
        tabla.addCell(celdaEncabezado("Peso", Element.ALIGN_RIGHT));
        tabla.addCell(celdaEncabezado("Km", Element.ALIGN_RIGHT));
        tabla.addCell(celdaEncabezado("Importe", Element.ALIGN_RIGHT));

        tabla.addCell(celdaCuerpo(datos.descripcion(), Element.ALIGN_LEFT, TEXTO_TABLA));
        tabla.addCell(celdaCuerpo(peso(datos.pesoKg()), Element.ALIGN_RIGHT, TEXTO_TABLA));
        tabla.addCell(celdaCuerpo(kilometros(datos.kilometros()), Element.ALIGN_RIGHT, TEXTO_TABLA));
        tabla.addCell(celdaCuerpo(moneda(datos.subtotal()), Element.ALIGN_RIGHT, TEXTO_TABLA_FUERTE));

        return tabla;
    }

    private PdfPTable tablaTotales(DatosRemito datos) {
        PdfPTable tabla = new PdfPTable(new float[] { 62f, 38f });
        tabla.setWidthPercentage(52);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);

        agregarFilaTotal(tabla, "Total kilos", peso(datos.pesoKg()), false);
        agregarFilaTotal(tabla, "Total kilómetros", kilometros(datos.kilometros()), false);
        if (datos.recargo() != null && datos.recargo().compareTo(BigDecimal.ZERO) > 0) {
            agregarFilaTotal(tabla, "Subtotal", moneda(datos.subtotal()), false);
            agregarFilaTotal(
                tabla,
                "Recargo por entrega fallida",
                moneda(datos.recargo()),
                false
            );
        }
        agregarFilaTotal(tabla, "Total a cobrar", moneda(datos.montoEnvio()), true);

        return tabla;
    }

    private PdfPTable cajaObservaciones() {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);

        PdfPCell celda = new PdfPCell(new Paragraph(" ", VALOR));
        celda.setMinimumHeight(48);
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.7f);
        celda.setPadding(6);
        tabla.addCell(celda);

        return tabla;
    }

    private PdfPTable tablaFirmas() {
        PdfPTable tabla = new PdfPTable(new float[] { 48f, 4f, 48f });
        tabla.setWidthPercentage(100);

        tabla.addCell(casillaFirma("Firma del receptor"));
        tabla.addCell(celdaVacia());
        tabla.addCell(casillaFirma("Aclaración y DNI"));

        return tabla;
    }

    private PdfPCell casillaFirma(String leyenda) {
        PdfPCell celda = new PdfPCell();
        celda.setMinimumHeight(72);
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.7f);
        celda.setPadding(8);
        celda.setVerticalAlignment(Element.ALIGN_BOTTOM);
        celda.addElement(new Paragraph(leyenda, ETIQUETA));
        return celda;
    }

    private PdfPCell bloque(String titulo) {
        PdfPCell celda = new PdfPCell();
        celda.setBackgroundColor(GRIS_CLARO);
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.7f);
        celda.setPadding(10);
        celda.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph etiqueta = new Paragraph(titulo, ETIQUETA);
        etiqueta.setSpacingAfter(5);
        celda.addElement(etiqueta);
        return celda;
    }

    private PdfPCell celdaEncabezado(String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, ENCABEZADO_TABLA));
        celda.setBackgroundColor(AZUL_NOCHE);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(7);
        return celda;
    }

    private PdfPCell celdaCuerpo(String texto, int alineacion, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.7f);
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(8);
        return celda;
    }

    private void agregarFilaTotal(
        PdfPTable tabla,
        String etiqueta,
        String valor,
        boolean destacada
    ) {
        Font fuente = destacada ? TEXTO_TABLA_FUERTE : TEXTO_TABLA;

        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, fuente));
        celdaEtiqueta.setHorizontalAlignment(Element.ALIGN_LEFT);
        celdaEtiqueta.setPadding(5);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, fuente));
        celdaValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaValor.setPadding(5);

        if (destacada) {
            for (PdfPCell celda : new PdfPCell[] { celdaEtiqueta, celdaValor }) {
                celda.setBorder(Rectangle.TOP);
                celda.setBorderColor(AZUL_NOCHE);
                celda.setBorderWidth(0.8f);
            }
        } else {
            celdaEtiqueta.setBorder(Rectangle.NO_BORDER);
            celdaValor.setBorder(Rectangle.NO_BORDER);
        }

        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }

    private Paragraph tituloSeccion(String texto) {
        Paragraph titulo = new Paragraph(texto, TITULO_SECCION);
        titulo.setSpacingAfter(2);
        return titulo;
    }

    private Paragraph espacio(float alto) {
        Paragraph parrafo = new Paragraph(" ", VALOR);
        parrafo.setLeading(alto);
        return parrafo;
    }

    private PdfPCell sinBorde() {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(0);
        return celda;
    }

    private PdfPCell celdaVacia() {
        PdfPCell celda = new PdfPCell(new Phrase(" ", VALOR));
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(0);
        return celda;
    }

    private Paragraph parrafoDerecha(String texto, Font fuente) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_RIGHT);
        return parrafo;
    }

    private String fecha(LocalDate valor) {
        return valor == null ? "Sin informar" : valor.format(FECHA);
    }

    private String peso(BigDecimal valor) {
        return numero(valor) + " kg";
    }

    private String kilometros(BigDecimal valor) {
        return valor == null ? "Sin informar" : numero(valor) + " km";
    }

    private String moneda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_AR).format(valor);
    }

    private String numero(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat(
            "#,##0.00",
            DecimalFormatSymbols.getInstance(LOCALE_AR)
        );
        return formato.format(valor);
    }

    private String estado(String valor) {
        return switch (valor) {
            case "EN_REVISION" -> "En revisión";
            case "EN_TRANSITO" -> "En tránsito";
            case "CANCELADO_EN_TRANSITO" -> "Cancelado en tránsito";
            case "PENDIENTE" -> "Pendiente";
            case "ENTREGADO" -> "Entregado";
            case "FALLIDO" -> "Fallido";
            default -> valor;
        };
    }

    private static final class PieDePagina extends PdfPageEventHelper {

        private final DatosRemito datos;

        private PieDePagina(DatosRemito datos) {
            this.datos = datos;
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document documento) {
            PdfContentByte lienzo = escritor.getDirectContent();
            float y = documento.bottomMargin() - 22;

            lienzo.setColorStroke(BORDE);
            lienzo.setLineWidth(0.5f);
            lienzo.moveTo(documento.leftMargin(), y + 12);
            lienzo.lineTo(documento.right(), y + 12);
            lienzo.stroke();

            ColumnText.showTextAligned(
                lienzo,
                Element.ALIGN_LEFT,
                new Phrase(
                    "Remito " + datos.numero() + " · Documento no válido como factura",
                    PIE
                ),
                documento.leftMargin(),
                y,
                0
            );
            ColumnText.showTextAligned(
                lienzo,
                Element.ALIGN_RIGHT,
                new Phrase("Página " + escritor.getPageNumber(), PIE),
                documento.right(),
                y,
                0
            );
        }
    }
}
