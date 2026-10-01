package com.scoreperu.api.client;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;

import java.util.List;

public interface ClienteConsultaExterna {
    Contribuyente obtenerDatosSunat(String ruc);
    List<Sancion> obtenerSancionesIndecopi(String ruc);
}
