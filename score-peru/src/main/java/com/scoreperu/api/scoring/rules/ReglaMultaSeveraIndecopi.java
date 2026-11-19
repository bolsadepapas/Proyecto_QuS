package com.scoreperu.api.scoring.rules;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import com.scoreperu.api.scoring.ReglaScoring;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReglaMultaSeveraIndecopi implements ReglaScoring {

    private static final String NOMBRE_REGLA = "R04 - Multas acumuladas graves en INDECOPI (>10 UIT: -15 pts, >=50 UIT: -25 pts)";
    private static final double UMBRAL_MODERADO_UIT = 10.0;
    private static final double UMBRAL_SEVERO_UIT = 50.0;
    private static final int PENALIZACION_MODERADA = 15;
    private static final int PENALIZACION_SEVERA = 25;

    @Override
    public boolean aplica(Contribuyente contribuyente, List<Sancion> sanciones) {
        return calcularTotalUit(sanciones) > UMBRAL_MODERADO_UIT;
    }

    @Override
    public int getPuntosPenalizacion() {
        return PENALIZACION_MODERADA;
    }

    @Override
    public int calcularPenalizacion(Contribuyente contribuyente, List<Sancion> sanciones) {
        double totalUit = calcularTotalUit(sanciones);
        if (totalUit >= UMBRAL_SEVERO_UIT) {
            return PENALIZACION_SEVERA;
        }
        if (totalUit > UMBRAL_MODERADO_UIT) {
            return PENALIZACION_MODERADA;
        }
        return 0;
    }

    private double calcularTotalUit(List<Sancion> sanciones) {
        if (sanciones == null || sanciones.isEmpty()) {
            return 0.0;
        }
        return sanciones.stream()
                .filter(s -> s != null && s.montoMultaUit() > 0)
                .mapToDouble(Sancion::montoMultaUit)
                .sum();
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
