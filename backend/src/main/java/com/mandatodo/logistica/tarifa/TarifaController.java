package com.mandatodo.logistica.tarifa;

import com.mandatodo.logistica.security.JwtAuthInterceptor;
import com.mandatodo.logistica.tarifa.dto.ActualizarTarifaRequest;
import com.mandatodo.logistica.tarifa.dto.TarifaConfigResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tarifas")
public class TarifaController {

    private final TarifaConfigService tarifaConfigService;

    public TarifaController(TarifaConfigService tarifaConfigService) {
        this.tarifaConfigService = tarifaConfigService;
    }

    @GetMapping
    public ResponseEntity<TarifaConfigResponse> obtener() {
        return ResponseEntity.ok(tarifaConfigService.obtener());
    }

    @PutMapping
    public ResponseEntity<TarifaConfigResponse> actualizar(
        @Valid @RequestBody ActualizarTarifaRequest request,
        @RequestAttribute(value = JwtAuthInterceptor.ATTR_ID_OPERARIO, required = false)
        Long idOperario,
        @RequestAttribute(value = JwtAuthInterceptor.ATTR_ROL, required = false)
        String rol
    ) {
        return ResponseEntity.ok(
            tarifaConfigService.actualizar(request, idOperario, rol)
        );
    }
}
