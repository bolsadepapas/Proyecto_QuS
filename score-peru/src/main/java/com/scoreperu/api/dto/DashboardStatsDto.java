package com.scoreperu.api.dto;

import java.util.Map;

/**
 * DTO para las estadísticas agregadas del Dashboard Nacional.
 * Se calcula a partir de los datos almacenados en consultas_ruc_cache.
 */
public record DashboardStatsDto(
    /** Total de empresas analizadas (registros únicos en caché) */
    long totalEmpresasAnalizadas,

    /** Total de consultas con al menos una regla activada */
    long empresasConHallazgos,

    /** Porcentaje de empresas con riesgo ALTO */
    double porcentajeRiesgoAlto,

    /** Score promedio nacional */
    double scorePromedio,

    /** Nivel de riesgo del promedio */
    String nivelRiesgoPromedio,

    /** Distribución por nivel de riesgo: {"BAJO": 46, "MEDIO": 36, "ALTO": 18} */
    Map<String, Long> distribucionRiesgo,

    /** Reglas más frecuentemente activadas: {"R01 - ...": 120, "R02 - ...": 85} */
    Map<String, Long> reglasTopActivadas
) {}
