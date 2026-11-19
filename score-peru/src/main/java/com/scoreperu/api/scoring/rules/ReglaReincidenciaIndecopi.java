package com.scoreperu.api.scoring.rules;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import com.scoreperu.api.scoring.ReglaScoring;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReglaReincidenciaIndecopi implements ReglaScoring {

    private static final String NOMBRE_REGLA = "R05 - Reincidencia sistemática de infracciones en INDECOPI (>=5 sanciones, hasta -20 pts)";
    private static final int UMBRAL_REINCIDENCIA = 5;
    private static final int UMBRAL_REINCIDENCIA_CRITICA = 10;
    private static final int PENALIZACION_BASE = 15;
    private static final int PENALIZACION_CRITICA = 20;

    @Override
    public boolean aplica(Contribuyente contribuyente, List<Sancion> sanciones) {
        return contarSanciones(sanciones) >= UMBRAL_REINCIDENCIA;
    }

    @Override
    public int getPuntosPenalizacion() {
        return PENALIZACION_BASE;
    }

    @Override
    public int calcularPenalizacion(Contribuyente contribuyente, List<Sancion> sanciones) {
        int total = contarSanciones(sanciones);
        if (total >= UMBRAL_REINCIDENCIA_CRITICA) {
            return PENALIZACION_CRITICA;
        }
        if (total >= UMBRAL_REINCIDENCIA) {
            return PENALIZACION_BASE;
        }
        return 0;
    }

    private int contarSanciones(List<Sancion> sanciones) {
        if (sanciones == null || sanciones.isEmpty()) {
            return 0;
        }
        return (int) sanciones.stream().filter(s -> s != null).count();
    }

    @Override
    public String getNombreRegla() {
        return NOMBRE_REGLA;
    }

    @Override
    public String getFuente() {
        return "INDECOPI";
    }
}
