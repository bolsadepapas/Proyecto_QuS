package com.scoreperu.api.scoring.rules;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import com.scoreperu.api.scoring.ReglaScoring;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReglaCondicionDomicilio implements ReglaScoring {

    private static final String CONDICION_NO_HABIDO = "NO HABIDO";
    private static final String CONDICION_NO_HALLADO = "NO HALLADO";

    @Override
    public boolean aplica(Contribuyente contribuyente, List<Sancion> sanciones) {
        if (contribuyente == null || contribuyente.condicion() == null) {
            return false;
        }
        String condicion = contribuyente.condicion().trim().toUpperCase();
        return CONDICION_NO_HABIDO.equals(condicion) || CONDICION_NO_HALLADO.equals(condicion);
    }

    @Override
    public int getPuntosPenalizacion() {
        return 25;
    }

    @Override
    public String getNombreRegla() {
        return "R02 - Condición de domicilio NO HABIDO / NO HALLADO (-25 pts)";
    }
}
