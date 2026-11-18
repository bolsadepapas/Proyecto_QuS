package com.scoreperu.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO principal de respuesta para la consulta de scoring.
 * Versión enriquecida que expone al frontend todo el detalle necesario
 * para las vistas de Resultado, Reporte PDF y Dashboard.
 */
public record ResponseReporteRiesgo(
    String ruc,
    String razonSocial,
    String estado,
    String condicion,
    int puntajeRiesgo,
    String nivelRiesgo,
    List<String> reglasActivadas,

    // --- Campos enriquecidos para el frontend ---

    /** Desglose individual de cada regla activada (código, fuente, puntos) */
    List<ReglaActivadaDto> detalleReglas,

    /** Lista de sanciones con entidad, motivo, fecha y monto */
    List<SancionDto> sanciones,

    /** Fecha de inicio de actividades del contribuyente */
    LocalDate fechaInicioActividades,

    /** Indica si el contribuyente tiene inhabilitación vigente */
    boolean inhabilitado,

    /** Timestamp de cuándo se realizó o actualizó la consulta */
    LocalDateTime fechaConsulta
) {
    /**
     * Constructor de compatibilidad retroactiva para cache hits
     * y tests existentes que usan la firma original de 7 campos.
     */
    public ResponseReporteRiesgo(
            String ruc, String razonSocial, String estado, String condicion,
            int puntajeRiesgo, String nivelRiesgo, List<String> reglasActivadas) {
        this(ruc, razonSocial, estado, condicion, puntajeRiesgo, nivelRiesgo,
                reglasActivadas, List.of(), List.of(), null, false, null);
    }
}
