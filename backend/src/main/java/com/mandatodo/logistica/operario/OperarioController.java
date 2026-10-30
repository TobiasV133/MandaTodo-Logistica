package com.mandatodo.logistica.operario;

import com.mandatodo.logistica.operario.dto.ActualizarUsuarioDTO;
import com.mandatodo.logistica.operario.dto.CrearUsuarioDTO;
import com.mandatodo.logistica.operario.dto.OperarioDTO;
import com.mandatodo.logistica.security.JwtAuthInterceptor;
import jakarta.validation.Valid;
import java.util.List;
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
        return ResponseEntity.ok(
            soloActivos ? operarioService.listarActivos() : operarioService.listar()
        );
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<OperarioDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(operarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<OperarioDTO> crear(
        @Valid @RequestBody CrearUsuarioDTO dto,
        @RequestAttribute(JwtAuthInterceptor.ATTR_ID_OPERARIO) Long idOperarioAuditor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(operarioService.crear(dto, idOperarioAuditor));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<OperarioDTO> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarUsuarioDTO dto,
        @RequestAttribute(JwtAuthInterceptor.ATTR_ID_OPERARIO) Long idOperarioAuditor
    ) {
        return ResponseEntity.ok(operarioService.actualizar(id, dto, idOperarioAuditor));
    }
}
