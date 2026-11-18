package com.scoreperu.api.dto;

/**
 * DTO que expone el detalle individual de cada regla de scoring activada.
 * Permite al frontend mostrar el desglose: código, descripción, fuente y penalización.
 */
public record ReglaActivadaDto(
    String codigo,
    String nombre,
    String fuente,
    int puntosPenalizacion
) {}

