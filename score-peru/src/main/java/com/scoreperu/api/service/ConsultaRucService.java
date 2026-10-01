package com.scoreperu.api.service;

import com.scoreperu.api.dto.ResponseReporteRiesgo;
import com.scoreperu.api.exception.RucInvalidoException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ConsultaRucService {

    public ResponseReporteRiesgo consultarScoringRuc(String ruc) {
        validarRuc(ruc);

        String rucLimpio = ruc.trim();

        // Determinación de tipo de contribuyente simulado para razón social
        String razonSocial = rucLimpio.startsWith("20") 
                ? "CORPORACION PERUANA DE SERVICIOS S.A.C." 
                : "JUAN PEREZ ROJAS";
        String estado = "ACTIVO";
        String condicion = "HABIDO";

        // Scoring simulado según el último dígito
        int ultimoDigito = Character.getNumericValue(rucLimpio.charAt(rucLimpio.length() - 1));
        boolean esPar = (ultimoDigito % 2 == 0);

        int puntajeRiesgo;
        String nivelRiesgo;
        List<String> reglasActivadas;

        if (esPar) {
            puntajeRiesgo = 0;
            nivelRiesgo = "BAJO";
            reglasActivadas = Collections.emptyList();
        } else {
            puntajeRiesgo = 80;
            nivelRiesgo = "ALTO";
            reglasActivadas = List.of("Sanción registrada en INDECOPI");
        }

        return new ResponseReporteRiesgo(
                rucLimpio,
                razonSocial,
                estado,
                condicion,
                puntajeRiesgo,
                nivelRiesgo,
                reglasActivadas
        );
    }

    private void validarRuc(String ruc) {
        if (ruc == null || ruc.trim().isEmpty()) {
            throw new RucInvalidoException("El RUC es obligatorio y no puede estar vacío.");
        }

        String rucLimpio = ruc.trim();

        if (rucLimpio.length() != 11 || !rucLimpio.matches("\\d{11}")) {
            throw new RucInvalidoException("El RUC debe tener exactamente 11 dígitos numéricos.");
        }

        if (!rucLimpio.startsWith("10") && !rucLimpio.startsWith("20")) {
            throw new RucInvalidoException("El RUC debe iniciar con los dígitos '10' o '20'.");
        }
    }
}
