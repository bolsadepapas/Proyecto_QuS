package com.scoreperu.api.client;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Component
public class ClienteConsultaExternaSimulado implements ClienteConsultaExterna {

    @Override
    public Contribuyente obtenerDatosSunat(String ruc) {
        String razonSocial = ruc.startsWith("20")
                ? "CORPORACION PERUANA DE SERVICIOS S.A.C."
                : "JUAN PEREZ ROJAS";
        String estado = "ACTIVO";
        String condicion = "HABIDO";
        return new Contribuyente(ruc, razonSocial, estado, condicion);
    }

    @Override
    public List<Sancion> obtenerSancionesIndecopi(String ruc) {
        int ultimoDigito = Character.getNumericValue(ruc.charAt(ruc.length() - 1));
        boolean esImpar = (ultimoDigito % 2 != 0);

        if (esImpar) {
            return List.of(new Sancion(
                    "SANC-2024-001",
                    "INDECOPI",
                    "Infracción al Código de Protección al Consumidor",
                    LocalDate.now().minusMonths(2)
            ));
        }
        return Collections.emptyList();
    }
}
