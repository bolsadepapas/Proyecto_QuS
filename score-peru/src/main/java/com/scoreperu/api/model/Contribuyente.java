package com.scoreperu.api.model;

import java.time.LocalDate;

public record Contribuyente(
    String ruc,
    String razonSocial,
    String estado,
    String condicion,
    boolean inhabilitado,
    LocalDate fechaInicioActividades
) {
    public Contribuyente(String ruc, String razonSocial, String estado, String condicion) {
        this(ruc, razonSocial, estado, condicion, false, LocalDate.now().minusYears(2));
    }
}
