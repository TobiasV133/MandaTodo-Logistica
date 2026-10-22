package com.mandatodo.backend.controller;

import com.mandatodo.backend.dto.cliente.ClienteResponse;
import com.mandatodo.backend.service.ClienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/buscar")
    public ResponseEntity<ClienteResponse> buscarPorDocumento(@RequestParam("doc") String documento) {
        return ResponseEntity.ok(clienteService.buscarPorDocumento(documento));
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }
}
