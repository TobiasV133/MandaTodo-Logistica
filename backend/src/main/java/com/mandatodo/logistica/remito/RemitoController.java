package com.mandatodo.logistica.remito;

import com.mandatodo.logistica.remito.dto.ImprimirRemitoRequest;
import com.mandatodo.logistica.security.JwtAuthInterceptor;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Emisión de remitos. Devuelve el PDF como descarga directa; la información de reimpresión viaja en
 * el documento y en los encabezados {@code X-Remito-*}.
 */
@RestController
@RequestMapping("/api/remitos")
public class RemitoController {

    private final RemitoService remitoService;

    public RemitoController(RemitoService remitoService) {
        this.remitoService = remitoService;
    }

    @PostMapping(value = "/{idPedido}/imprimir", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> imprimir(
        @PathVariable Long idPedido,
        @Valid @RequestBody ImprimirRemitoRequest request,
        @RequestAttribute(value = JwtAuthInterceptor.ATTR_ID_OPERARIO, required = false)
        Long operarioId
    ) {
        RemitoGenerado remito = remitoService.imprimir(
            idPedido,
            request,
            operarioId != null ? operarioId : request.operarioId()
        );

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .cacheControl(CacheControl.noStore())
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(remito.nombreArchivo())
                    .build()
                    .toString()
            )
            .header("X-Remito-Numero", remito.numero())
            .header("X-Remito-Reimpresion", Boolean.toString(remito.reimpresion()))
            .body(remito.contenido());
    }
}
