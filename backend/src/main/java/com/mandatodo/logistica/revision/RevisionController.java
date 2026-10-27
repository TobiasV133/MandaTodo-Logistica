package com.mandatodo.logistica.revision;

import com.mandatodo.logistica.pedido.dto.PedidoResponse;
import com.mandatodo.logistica.revision.dto.ResolverRevisionRequest;
import com.mandatodo.logistica.security.JwtAuthInterceptor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class RevisionController {

    private final RevisionService revisionService;

    public RevisionController(RevisionService revisionService) {
        this.revisionService = revisionService;
    }

    @GetMapping("/en-revision")
    public List<PedidoResponse> listarEnRevision() {
        return revisionService.listarEnRevision();
    }

    @PutMapping("/{id}/revision")
    public PedidoResponse resolver(
        @PathVariable Long id,
        @Valid @RequestBody ResolverRevisionRequest request,
        @RequestAttribute(value = JwtAuthInterceptor.ATTR_ID_OPERARIO, required = false)
        Long operarioId
    ) {
        return revisionService.resolver(
            id,
            request,
            operarioId != null ? operarioId : request.operarioId()
        );
    }
}
