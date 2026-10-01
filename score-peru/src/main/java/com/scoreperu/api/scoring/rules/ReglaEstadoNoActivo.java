package com.scoreperu.api.scoring.rules;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import com.scoreperu.api.scoring.ReglaScoring;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReglaEstadoNoActivo implements ReglaScoring {

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final int PUNTOS_PENALIZACION = 40;
    private static final String NOMBRE_REGLA = "R01 - Estado del contribuyente distinto de ACTIVO (-40 pts)";

    @Override
    public boolean aplica(Contribuyente contribuyente, List<Sancion> sanciones) {
        if (contribuyente == null || contribuyente.estado() == null) {
            return true;
        }
        return !ESTADO_ACTIVO.equalsIgnoreCase(contribuyente.estado().trim());
    }

    @Override
    public int getPuntosPenalizacion() {
        return PUNTOS_PENALIZACION;
    }

    @Override
    public String getNombreRegla() {
        return NOMBRE_REGLA;
    }
}
