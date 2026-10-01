package com.scoreperu.api.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    int status,
    String error,
    String mensaje,
    LocalDateTime timestamp
) {}
