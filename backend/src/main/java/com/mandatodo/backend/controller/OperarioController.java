package com.mandatodo.backend.controller;

import com.mandatodo.backend.dto.ActualizarUsuarioDTO;
import com.mandatodo.backend.dto.CrearUsuarioDTO;
import com.mandatodo.backend.dto.OperarioDTO;
import com.mandatodo.backend.service.OperarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de consulta y ABM de operarios.
 *
 * <p>La ruta canónica de la spec es {@code /api/usuarios}. Se mantiene
 * {@code /api/operarios} como alias compatible.</p>
 */
@RestController
@RequestMapping({"/api/usuarios", "/api/operarios"})
public class OperarioController {

    private final OperarioService operarioService;

    public OperarioController(OperarioService operarioService) {
        this.operarioService = operarioService;
    }

    @GetMapping
    public ResponseEntity<List<OperarioDTO>> listar(
            @RequestParam(name = "soloActivos", defaultValue = "false") boolean soloActivos
    ) {
        List<OperarioDTO> operarios = soloActivos
                ? operarioService.listarActivos()
                : operarioService.listar();
        return ResponseEntity.ok(operarios);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<OperarioDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(operarioService.buscarPorId(id));
    }

    /**
     * Alta de operario. Responde con el estándar HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<OperarioDTO> crear(
            @Valid @RequestBody CrearUsuarioDTO dto,
            @RequestAttribute(name = "idOperario", required = false) Integer idOperarioAuditor
    ) {
        OperarioDTO creado = operarioService.crear(dto, idOperarioAuditor);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<OperarioDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarUsuarioDTO dto,
            @RequestAttribute(name = "idOperario", required = false) Integer idOperarioAuditor
    ) {
        return ResponseEntity.ok(operarioService.actualizar(id, dto, idOperarioAuditor));
    }
}
