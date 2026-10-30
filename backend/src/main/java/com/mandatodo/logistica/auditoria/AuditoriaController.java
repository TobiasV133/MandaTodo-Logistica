package com.mandatodo.logistica.auditoria;

import com.mandatodo.logistica.auditoria.dto.AuditoriaDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/auditoria", "/api/auditorias"})
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<List<AuditoriaDTO>> listar() {
        return ResponseEntity.ok(auditoriaService.listar());
    }
}
