package com.scoreperu.api.scoring;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;

import java.util.List;

public interface ReglaScoring {
    boolean aplica(Contribuyente contribuyente, List<Sancion> sanciones);
    int getPuntosPenalizacion();
    String getNombreRegla();

    /**
     * Calcula dinámicamente la penalización en función de los datos del contribuyente
     * y la lista de sanciones evaluadas (por ejemplo, reglas acumulativas con tope).
     */
    default int calcularPenalizacion(Contribuyente contribuyente, List<Sancion> sanciones) {
        return getPuntosPenalizacion();
    }

    default int getPuntosRiesgo() {
        return getPuntosPenalizacion();
    }

    /**
     * Código corto de la regla (ej. "R01", "R02").
     * Usado por el frontend para identificar y mostrar cada regla individualmente.
     */
    default String getCodigoRegla() {
        // Extrae el código del nombre si sigue el patrón "R## - ..."
        String nombre = getNombreRegla();
        if (nombre != null && nombre.length() >= 3) {
            int separador = nombre.indexOf(" - ");
            if (separador > 0) {
                return nombre.substring(0, separador).trim();
            }
        }
        return "R??";
    }

    /**
     * Fuente de datos que esta regla evalúa (ej. "SUNAT", "INDECOPI").
     */
    default String getFuente() {
        return "SUNAT";
    }
}
