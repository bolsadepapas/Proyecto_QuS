package com.scoreperu.api.controller;

import com.scoreperu.api.dto.RequestConsultaRuc;
import com.scoreperu.api.dto.ResponseReporteRiesgo;
import com.scoreperu.api.service.ConsultaRucService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/consulta")
@SuppressWarnings("null")
public class ConsultaController {

    private final ConsultaRucService consultaRucService;

    public ConsultaController(ConsultaRucService consultaRucService) {
        this.consultaRucService = consultaRucService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<ResponseReporteRiesgo>> consultarRucPost(
            @Valid @RequestBody RequestConsultaRuc request) {
        return consultaRucService.consultarScoringRucAsync(request.ruc())
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{ruc}")
    public CompletableFuture<ResponseEntity<ResponseReporteRiesgo>> consultarRucGet(
            @PathVariable String ruc) {
        return consultaRucService.consultarScoringRucAsync(ruc)
                .thenApply(ResponseEntity::ok);
    }
}
