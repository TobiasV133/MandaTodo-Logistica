package com.mandatodo.backend.controller;

import com.mandatodo.backend.dto.AuditoriaDTO;
import com.mandatodo.backend.dto.RegistrarAuditoriaDTO;
import com.mandatodo.backend.exception.ApiException;
import com.mandatodo.backend.model.TipoEvento;
import com.mandatodo.backend.service.AuditoriaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API REST de consulta y registro de auditoría.
 *
 * <p>La ruta canónica de la spec es {@code /api/auditoria}. Se mantiene
 * {@code /api/auditorias} como alias compatible.</p>
 */
@RestController
@RequestMapping({"/api/auditoria", "/api/auditorias"})
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<List<AuditoriaDTO>> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer operario,
            @RequestParam(required = false) String tipoEvento
    ) {
        return ResponseEntity.ok(
                auditoriaService.listar(fecha, operario, parseTipoEvento(tipoEvento))
        );
    }

    @GetMapping("/operario/{idOperario:\\d+}")
    public ResponseEntity<List<AuditoriaDTO>> listarPorOperario(
            @PathVariable Integer idOperario
    ) {
        return ResponseEntity.ok(auditoriaService.listarPorOperario(idOperario));
    }

    @PostMapping
    public ResponseEntity<AuditoriaDTO> registrar(
            @Valid @RequestBody RegistrarAuditoriaDTO dto,
            @RequestAttribute(name = "idOperario", required = false) Integer idOperario
    ) {
        AuditoriaDTO creada = auditoriaService.registrarEvento(
                idOperario,
                dto.idPedido(),
                dto.actividad(),
                parseTipoEvento(dto.tipoEvento())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    private static TipoEvento parseTipoEvento(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return TipoEvento.desdeApi(valor);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
