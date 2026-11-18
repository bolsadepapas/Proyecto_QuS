package com.scoreperu.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Endpoint de estado del servicio (Health Check y raíz informativa).
 */
@RestController
@SuppressWarnings("null")
public class HealthController {

    @GetMapping({"/", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "servicio", "Score Perú Backend API",
                "estado", "UP",
                "puerto", 8080,
                "timestamp", LocalDateTime.now().toString(),
                "endpoints", Map.of(
                        "consultaGet", "/api/v1/consulta/{ruc}",
                        "consultaPost", "/api/v1/consulta",
                        "dashboardStats", "/api/v1/dashboard/stats"
                )
        ));
    }
}

