package com.scoreperu.api.scoring;

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

        for (ReglaScoring regla : reglas) {
            if (regla.aplica(contribuyente, sanciones)) {
                totalPenalizaciones += regla.getPuntosPenalizacion();
                reglasActivadas.add(regla.getNombreRegla());
            }
        }

        // TODAS LAS EMPRESAS EMPIEZAN CON UN SCORE DE 100 pts.
        // 0 de puntaje es la peor de las empresas (máximo riesgo).
        int scoreFinal = Math.max(0, Math.min(100, SCORE_BASE_INICIAL - totalPenalizaciones));
        String nivelRiesgo = determinarNivelRiesgo(scoreFinal);

        return new ResultadoScoring(scoreFinal, nivelRiesgo, reglasActivadas);
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
        List<String> reglasActivadas
    ) {}
}
