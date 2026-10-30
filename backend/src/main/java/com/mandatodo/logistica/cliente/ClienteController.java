package com.mandatodo.logistica.cliente;

import com.mandatodo.logistica.cliente.dto.ActualizarClienteDTO;
import com.mandatodo.logistica.cliente.dto.ClienteDTO;
import com.mandatodo.logistica.cliente.dto.ClienteResponse;
import com.mandatodo.logistica.cliente.dto.CrearClienteDTO;
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
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listar();
    }

    @GetMapping("/buscar")
    public ClienteResponse buscar(@RequestParam("doc") String documento) {
        return clienteService.buscarPorDocumento(documento);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ClienteDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ClienteDTO> crear(
        @Valid @RequestBody CrearClienteDTO dto,
        @RequestAttribute(JwtAuthInterceptor.ATTR_ID_OPERARIO) Long idOperario
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(clienteService.crear(dto, idOperario));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ClienteDTO> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarClienteDTO dto,
        @RequestAttribute(JwtAuthInterceptor.ATTR_ID_OPERARIO) Long idOperario
    ) {
        return ResponseEntity.ok(clienteService.actualizar(id, dto, idOperario));
    }
}
