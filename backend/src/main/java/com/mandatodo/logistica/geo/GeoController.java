package com.mandatodo.logistica.geo;

import com.mandatodo.logistica.geo.dto.NormalizacionResponse;
import com.mandatodo.logistica.geo.dto.NormalizarRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geo")
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    @PostMapping("/normalizar")
    public ResponseEntity<NormalizacionResponse> normalizar(
        @Valid @RequestBody NormalizarRequest request
    ) {
        return ResponseEntity.ok(geoService.normalizar(request));
    }
}
