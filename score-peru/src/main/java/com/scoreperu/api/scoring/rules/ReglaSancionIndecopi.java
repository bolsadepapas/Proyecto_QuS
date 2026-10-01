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

    @Override
    public boolean aplica(Contribuyente contribuyente, List<Sancion> sanciones) {
        if (sanciones == null || sanciones.isEmpty()) {
            return false;
        }
        LocalDate hace12Meses = LocalDate.now().minusMonths(12);
        return sanciones.stream()
                .anyMatch(s -> s.fechaSancion() != null && s.fechaSancion().isAfter(hace12Meses));
    }

    @Override
    public int getPuntosPenalizacion() {
        // En base a la tabla: -10 pts por sanción reciente
        return 10;
    }

    @Override
    public String getNombreRegla() {
        return NOMBRE_REGLA;
    }
}
