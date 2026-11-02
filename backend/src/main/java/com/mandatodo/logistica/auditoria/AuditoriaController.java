package com.mandatodo.logistica.auditoria;

import com.mandatodo.logistica.auditoria.dto.AuditoriaDTO;
import com.mandatodo.logistica.auditoria.dto.RegistrarAuditoriaRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/auditoria", "/api/auditorias"})
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<List<AuditoriaDTO>> listar(
        @RequestParam(required = false) LocalDate fecha,
        @RequestParam(required = false) LocalDate desde,
        @RequestParam(required = false) LocalDate hasta,
        @RequestParam(required = false) Long operario,
        @RequestParam(required = false) String tipoEvento
    ) {
        return ResponseEntity.ok(
            auditoriaService.listar(fecha, desde, hasta, operario, tipoEvento)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditoriaDTO registrar(@Valid @RequestBody RegistrarAuditoriaRequest request) {
        return auditoriaService.registrar(request);
    }
}
