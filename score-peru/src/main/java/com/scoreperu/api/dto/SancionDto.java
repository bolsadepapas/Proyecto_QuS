package com.scoreperu.api.dto;

import java.time.LocalDate;

/**
 * DTO que expone el detalle de cada sanción al frontend.
 * Mapea directamente desde el modelo de dominio Sancion.
 */
public record SancionDto(
    String idSancion,
    String entidad,
    String motivo,
    LocalDate fechaSancion,
    double montoMultaUit
) {}

