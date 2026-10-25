package com.mandatodo.backend.controller;

import com.mandatodo.backend.dto.pedido.PedidoRequest;
import com.mandatodo.backend.dto.pedido.PedidoResponse;
import com.mandatodo.backend.dto.pedido.ResumenPedidosResponse;
import com.mandatodo.backend.model.EstadoPedido;
import com.mandatodo.backend.security.JwtAuthInterceptor;
import com.mandatodo.backend.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(
            @Valid @RequestBody PedidoRequest request,
            @RequestAttribute(JwtAuthInterceptor.ATTR_ID_OPERARIO) Integer idOperario
    ) {
        return pedidoService.crear(request, idOperario);
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listar(
            @RequestParam(name = "estado", required = false) EstadoPedido estado
    ) {
        return ResponseEntity.ok(pedidoService.listar(estado));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenPedidosResponse> resumen() {
        return ResponseEntity.ok(pedidoService.resumen());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.obtener(id));
    }
}
