package com.mandatodo.logistica.pedido;

import com.mandatodo.logistica.pedido.dto.CambiarEstadoRequest;
import com.mandatodo.logistica.pedido.dto.CambioEstadoResponse;
import com.mandatodo.logistica.pedido.dto.CrearPedidoRequest;
import com.mandatodo.logistica.pedido.dto.CrearPedidoResponse;
import com.mandatodo.logistica.pedido.dto.PedidoResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CrearPedidoResponse crear(@Valid @RequestBody CrearPedidoRequest request) {
        return pedidoService.crear(request);
    }

    @GetMapping
    public List<PedidoResponse> listar(
        @RequestParam(required = false) EstadoPedido estado
    ) {
        return pedidoService.listar(estado);
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }

    @PatchMapping("/{id}/estado")
    public CambioEstadoResponse cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody CambiarEstadoRequest request
    ) {
        return pedidoService.cambiarEstado(id, request);
    }
}
