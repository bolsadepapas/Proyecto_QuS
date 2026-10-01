package com.scoreperu.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestConsultaRuc(
    @NotBlank(message = "El RUC no puede estar vacío")
    String ruc
) {}
