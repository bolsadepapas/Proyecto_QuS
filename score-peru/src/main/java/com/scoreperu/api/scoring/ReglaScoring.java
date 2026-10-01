package com.scoreperu.api.scoring;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;

import java.util.List;

public interface ReglaScoring {
    boolean aplica(Contribuyente contribuyente, List<Sancion> sanciones);
    int getPuntosPenalizacion();
    String getNombreRegla();

    default int getPuntosRiesgo() {
        return getPuntosPenalizacion();
    }
}
