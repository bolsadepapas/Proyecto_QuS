package com.scoreperu.api.client;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;

import java.util.List;

public interface ClienteConsultaExterna {
    Contribuyente obtenerDatosSunat(String ruc);
    List<Sancion> obtenerSancionesIndecopi(String ruc);

    /**
     * Obtiene en una sola operación agregada los datos de SUNAT e INDECOPI.
     * Las implementaciones que cuentan con un endpoint unificado (como Api QuS /api/v1/consulta/{ruc})
     * sobrescriben este método para evitar llamadas HTTP secuenciales redundantes.
     */
    default DatosExternosRuc obtenerDatosIntegrados(String ruc) {
        return null;
    }

    record DatosExternosRuc(Contribuyente contribuyente, List<Sancion> sanciones) {}
}
