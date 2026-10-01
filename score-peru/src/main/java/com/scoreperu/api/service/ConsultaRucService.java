package com.scoreperu.api.service;

import com.scoreperu.api.client.ClienteConsultaExterna;
import com.scoreperu.api.dto.ResponseReporteRiesgo;
import com.scoreperu.api.exception.RucInvalidoException;
import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import com.scoreperu.api.scoring.MotorScoringService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsultaRucService {

    private final ClienteConsultaExterna clienteConsultaExterna;
    private final MotorScoringService motorScoringService;

    public ConsultaRucService(ClienteConsultaExterna clienteConsultaExterna,
                              MotorScoringService motorScoringService) {
        this.clienteConsultaExterna = clienteConsultaExterna;
        this.motorScoringService = motorScoringService;
    }

    public ResponseReporteRiesgo consultarScoringRuc(String ruc) {
        validarRuc(ruc);

        String rucLimpio = ruc.trim();

        // 1. Obtención de datos mediante interfaz desacoplada (DIP)
        Contribuyente contribuyente = clienteConsultaExterna.obtenerDatosSunat(rucLimpio);
        List<Sancion> sanciones = clienteConsultaExterna.obtenerSancionesIndecopi(rucLimpio);

        // 2. Evaluación dinámica a través del Motor de Scoring (Base 100 - penalizaciones)
        MotorScoringService.ResultadoScoring resultadoScoring = motorScoringService.evaluar(contribuyente, sanciones);

        // 3. Mapeo al reporte de riesgo
        return new ResponseReporteRiesgo(
                contribuyente.ruc(),
                contribuyente.razonSocial(),
                contribuyente.estado(),
                contribuyente.condicion(),
                resultadoScoring.scoreFinal(),
                resultadoScoring.nivelRiesgo(),
                resultadoScoring.reglasActivadas()
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
