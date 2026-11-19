package com.scoreperu.api.scoring.rules;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import com.scoreperu.api.scoring.ReglaScoring;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ReglaSancionIndecopi implements ReglaScoring {

    private static final String NOMBRE_REGLA = "R03 - Sanciones recientes registradas en INDECOPI (-10 pts c/u, tope 30)";
    private static final int PUNTOS_POR_SANCION = 10;
    private static final int TOPE_PENALIZACION = 30;

    @Override
    public boolean aplica(Contribuyente contribuyente, List<Sancion> sanciones) {
        return contarSancionesRecientes(sanciones) > 0;
    }

    @Override
    public int getPuntosPenalizacion() {
        // Penalización base por sanción reciente individual
        return PUNTOS_POR_SANCION;
    }

    @Override
    public int calcularPenalizacion(Contribuyente contribuyente, List<Sancion> sanciones) {
        int sancionRecienteCount = contarSancionesRecientes(sanciones);
        if (sancionRecienteCount <= 0) {
            return 0;
        }
        return Math.min(TOPE_PENALIZACION, sancionRecienteCount * PUNTOS_POR_SANCION);
    }

    private int contarSancionesRecientes(List<Sancion> sanciones) {
        if (sanciones == null || sanciones.isEmpty()) {
            return 0;
        }
        LocalDate hace12Meses = LocalDate.now().minusMonths(12);
        LocalDate pisoPeriodoEvaluado = LocalDate.of(2024, 1, 1);
        LocalDate fechaCorte = hace12Meses.isBefore(pisoPeriodoEvaluado) ? hace12Meses : pisoPeriodoEvaluado;
        return (int) sanciones.stream()
                .filter(s -> s != null && s.fechaSancion() != null && !s.fechaSancion().isBefore(fechaCorte))
                .count();
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
