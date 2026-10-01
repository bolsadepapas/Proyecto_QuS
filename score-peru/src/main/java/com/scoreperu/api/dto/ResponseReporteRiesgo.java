package com.scoreperu.api.dto;

import java.util.List;

public record ResponseReporteRiesgo(
    String ruc,
    String razonSocial,
    String estado,
    String condicion,
    int puntajeRiesgo,
    String nivelRiesgo,
    List<String> reglasActivadas
) {}
