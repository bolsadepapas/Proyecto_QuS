package com.scoreperu.api.controller;

import com.scoreperu.api.dto.RequestConsultaRuc;
import com.scoreperu.api.dto.ResponseReporteRiesgo;
import com.scoreperu.api.service.ConsultaRucService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consulta")
public class ConsultaController {

    private final ConsultaRucService consultaRucService;

    public ConsultaController(ConsultaRucService consultaRucService) {
        this.consultaRucService = consultaRucService;
    }

    @PostMapping
    public ResponseEntity<ResponseReporteRiesgo> consultarRucPost(@Valid @RequestBody RequestConsultaRuc request) {
        ResponseReporteRiesgo reporte = consultaRucService.consultarScoringRuc(request.ruc());
        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/{ruc}")
    public ResponseEntity<ResponseReporteRiesgo> consultarRucGet(@PathVariable String ruc) {
        ResponseReporteRiesgo reporte = consultaRucService.consultarScoringRuc(ruc);
        return ResponseEntity.ok(reporte);
    }
}
