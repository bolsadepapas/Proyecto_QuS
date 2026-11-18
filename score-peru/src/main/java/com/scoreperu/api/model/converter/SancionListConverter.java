package com.scoreperu.api.model.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scoreperu.api.dto.SancionDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class SancionListConverter implements AttributeConverter<List<SancionDto>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final TypeReference<List<SancionDto>> LIST_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<SancionDto> sanciones) {
        if (sanciones == null || sanciones.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(sanciones);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public List<SancionDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(dbData, LIST_TYPE);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}

