package com.scoreperu.api.controller;

import com.scoreperu.api.dto.DashboardStatsDto;
import com.scoreperu.api.repository.ConsultaRucRepository;
import com.scoreperu.api.scoring.MotorScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoint de Dashboard Nacional.
 * Genera estadísticas agregadas mediante consultas @Query (COUNT, AVG y GROUP BY)
 * directamente en la base de datos sin hidratar entidades completas en memoria JVM.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@SuppressWarnings("null")
public class DashboardController {

    private static final String DELIMITADOR_REGLAS_REGEX = "\\|\\|";

    private final ConsultaRucRepository consultaRucRepository;
    private final MotorScoringService motorScoringService;

    public DashboardController(ConsultaRucRepository consultaRucRepository,
                                MotorScoringService motorScoringService) {
        this.consultaRucRepository = consultaRucRepository;
        this.motorScoringService = motorScoringService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> obtenerEstadisticas() {
        long total = consultaRucRepository.contarTotalEmpresas();

        if (total == 0) {
            return ResponseEntity.ok(new DashboardStatsDto(
                    0, 0, 0.0, 0.0, "N/A",
                    Map.of("BAJO", 0L, "MEDIO", 0L, "ALTO", 0L),
                    Map.of()
            ));
        }

        // Empresas con al menos una regla activada calculado en BD con COUNT
        long conHallazgos = consultaRucRepository.contarEmpresasConHallazgos();

        // Distribución por nivel de riesgo calculada en BD con GROUP BY
        Map<String, Long> distribucion = new LinkedHashMap<>();
        distribucion.put("BAJO", 0L);
        distribucion.put("MEDIO", 0L);
        distribucion.put("ALTO", 0L);

        List<Object[]> filasDistribucion = consultaRucRepository.contarDistribucionPorNivelRiesgo();
        for (Object[] fila : filasDistribucion) {
            if (fila != null && fila.length >= 2 && fila[0] != null && fila[1] != null) {
                String nivel = String.valueOf(fila[0]).trim().toUpperCase();
                long cantidad = ((Number) fila[1]).longValue();
                distribucion.put(nivel, cantidad);
            }
        }

        // Porcentaje riesgo alto
        long riesgoAlto = distribucion.getOrDefault("ALTO", 0L);
        double porcentajeAlto = (riesgoAlto * 100.0) / total;

        // Score promedio nacional calculado en BD con AVG
        Double promedioDb = consultaRucRepository.obtenerScorePromedio();
        double scorePromedio = (promedioDb != null) ? promedioDb : 0.0;
        String nivelPromedio = motorScoringService.determinarNivelRiesgo((int) Math.round(scorePromedio));

        // Top reglas activadas agregadas desde combinaciones únicas (GROUP BY en SQL)
        Map<String, Long> conteoPorRegla = new HashMap<>();
        List<Object[]> combinaciones = consultaRucRepository.contarFrecuenciaCombinacionesReglas();
        for (Object[] fila : combinaciones) {
            if (fila != null && fila.length >= 2 && fila[0] != null && fila[1] != null) {
                String cadenaReglas = String.valueOf(fila[0]);
                long frecuencia = ((Number) fila[1]).longValue();
                for (String token : cadenaReglas.split(DELIMITADOR_REGLAS_REGEX)) {
                    String reglaLimpia = token.trim();
                    if (!reglaLimpia.isEmpty()) {
                        conteoPorRegla.merge(reglaLimpia, frecuencia, Long::sum);
                    }
                }
            }
        }

        Map<String, Long> reglasTop = conteoPorRegla.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new
                ));

        DashboardStatsDto stats = new DashboardStatsDto(
                total,
                conHallazgos,
                Math.round(porcentajeAlto * 10.0) / 10.0,
                Math.round(scorePromedio * 10.0) / 10.0,
                nivelPromedio,
                distribucion,
                reglasTop
        );

        return ResponseEntity.ok(stats);
    }
}
