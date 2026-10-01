package com.scoreperu.api.scoring.rules;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import com.scoreperu.api.scoring.ReglaScoring;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReglaInhabilitacionVigente implements ReglaScoring {

    @Override
    public boolean aplica(Contribuyente contribuyente, List<Sancion> sanciones) {
        return contribuyente != null && contribuyente.inhabilitado();
    }

    @Override
    public int getPuntosPenalizacion() {
        return 30;
    }

    @Override
    public String getNombreRegla() {
        return "R08 - Inhabilitación vigente (-30 pts)";
    }
}
