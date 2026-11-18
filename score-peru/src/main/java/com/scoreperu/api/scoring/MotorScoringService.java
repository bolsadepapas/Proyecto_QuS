package com.scoreperu.api.scoring;

import com.scoreperu.api.dto.ReglaActivadaDto;
import com.scoreperu.api.dto.SancionDto;
import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MotorScoringService {

    public static final int SCORE_BASE_INICIAL = 100;

    private final List<ReglaScoring> reglas;

    public MotorScoringService(List<ReglaScoring> reglas) {
        this.reglas = (reglas != null) ? reglas : List.of();
    }

    public ResultadoScoring evaluar(Contribuyente contribuyente, List<Sancion> sanciones) {
        int totalPenalizaciones = 0;
        List<String> reglasActivadas = new ArrayList<>();
        List<ReglaActivadaDto> detalleReglas = new ArrayList<>();

        for (ReglaScoring regla : reglas) {
            if (regla.aplica(contribuyente, sanciones)) {
                int puntosPenalizacion = regla.calcularPenalizacion(contribuyente, sanciones);
                totalPenalizaciones += puntosPenalizacion;
                reglasActivadas.add(regla.getNombreRegla());
                detalleReglas.add(new ReglaActivadaDto(
                        regla.getCodigoRegla(),
                        regla.getNombreRegla(),
                        regla.getFuente(),
                        puntosPenalizacion
                ));
            }
        }

        // TODAS LAS EMPRESAS EMPIEZAN CON UN SCORE DE 100 pts.
        // 0 de puntaje es la peor de las empresas (máximo riesgo).
        int scoreFinal = Math.max(0, Math.min(100, SCORE_BASE_INICIAL - totalPenalizaciones));
        String nivelRiesgo = determinarNivelRiesgo(scoreFinal);

        return new ResultadoScoring(scoreFinal, nivelRiesgo, reglasActivadas, detalleReglas);
    }

    /**
     * Reconstruye los DTOs detallados de las reglas activadas a partir de sus nombres
     * almacenados en caché, correlacionando con las estrategias registradas.
     */
    public List<ReglaActivadaDto> reconstruirDetalleReglas(List<String> nombresReglas) {
        return reconstruirDetalleReglas(nombresReglas, List.of());
    }

    public List<ReglaActivadaDto> reconstruirDetalleReglas(List<String> nombresReglas, List<SancionDto> sancionesDto) {
        if (nombresReglas == null || nombresReglas.isEmpty()) {
            return List.of();
        }
        List<Sancion> sancionesDominio = (sancionesDto == null) ? List.of() : sancionesDto.stream()
                .map(s -> new Sancion(s.idSancion(), s.entidad(), s.motivo(), s.fechaSancion(), s.montoMultaUit()))
                .toList();

        List<ReglaActivadaDto> resultado = new ArrayList<>();
        for (String nombre : nombresReglas) {
            if (nombre == null || nombre.isBlank()) {
                continue;
            }
            ReglaScoring coincidencia = reglas.stream()
                    .filter(r -> nombre.equals(r.getNombreRegla()))
                    .findFirst()
                    .orElse(null);

            if (coincidencia != null) {
                int penalizacion = sancionesDominio.isEmpty()
                        ? coincidencia.getPuntosPenalizacion()
                        : coincidencia.calcularPenalizacion(null, sancionesDominio);
                if (penalizacion <= 0) {
                    penalizacion = coincidencia.getPuntosPenalizacion();
                }
                resultado.add(new ReglaActivadaDto(
                        coincidencia.getCodigoRegla(),
                        coincidencia.getNombreRegla(),
                        coincidencia.getFuente(),
                        penalizacion
                ));
            } else {
                String codigo = "R??";
                int separador = nombre.indexOf(" - ");
                if (separador > 0) {
                    codigo = nombre.substring(0, separador).trim();
                }
                String fuente = nombre.toUpperCase().contains("INDECOPI") ? "INDECOPI" : "SUNAT";
                resultado.add(new ReglaActivadaDto(codigo, nombre, fuente, 0));
            }
        }
        return resultado;
    }

    public String determinarNivelRiesgo(int score) {
        if (score >= 80) {
            return "BAJO";
        } else if (score >= 50) {
            return "MEDIO";
        } else {
            return "ALTO";
        }
    }

    public record ResultadoScoring(
            int scoreFinal,
            String nivelRiesgo,
            List<String> reglasActivadas,
            List<ReglaActivadaDto> detalleReglas) {
    }
}
