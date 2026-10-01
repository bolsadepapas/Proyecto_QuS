package com.scoreperu.api.model;

import java.time.LocalDate;

public record Sancion(
    String idSancion,
    String entidad,
    String motivo,
    LocalDate fechaSancion,
    double montoMultaUit
) {
    public Sancion(String idSancion, String entidad, String motivo, LocalDate fechaSancion) {
        this(idSancion, entidad, motivo, fechaSancion, 1.0);
    }
}
