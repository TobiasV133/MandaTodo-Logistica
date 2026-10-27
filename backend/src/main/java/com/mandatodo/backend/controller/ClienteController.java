package com.mandatodo.backend.controller;

import com.mandatodo.backend.dto.ActualizarClienteDTO;
import com.mandatodo.backend.dto.ClienteDTO;
import com.mandatodo.backend.dto.CrearClienteDTO;
import com.mandatodo.backend.dto.cliente.ClienteResponse;
import com.mandatodo.backend.service.ClienteService;
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
 * API REST de la libreta digital de clientes.
 *
 * <p>Las rutas {@code GET /buscar} y {@code GET} conservan el contrato del
 * módulo de carga. {@code GET /{id}}, {@code POST} y {@code PUT /{id}}
 * agregan el ABM de clientes.</p>
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/buscar")
    public ResponseEntity<ClienteResponse> buscarPorDocumento(
            @RequestParam("doc") String documento
    ) {
        return ResponseEntity.ok(clienteService.buscarPorDocumento(documento));
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ClienteDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ClienteDTO> crear(
            @Valid @RequestBody CrearClienteDTO dto,
            @RequestAttribute(name = "idOperario", required = false) Integer idOperario
    ) {
        ClienteDTO creado = clienteService.crear(dto, idOperario);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ClienteDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarClienteDTO dto,
            @RequestAttribute(name = "idOperario", required = false) Integer idOperario
    ) {
        return ResponseEntity.ok(clienteService.actualizar(id, dto, idOperario));
    }
}
