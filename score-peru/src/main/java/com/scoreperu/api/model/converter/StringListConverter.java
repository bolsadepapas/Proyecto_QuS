package com.scoreperu.api.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Converter
@SuppressWarnings("null")
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String JOIN_DELIMITER = "||";
    private static final String SPLIT_REGEX = "\\|\\|";

    @Override
    public String convertToDatabaseColumn(List<String> stringList) {
        if (stringList == null || stringList.isEmpty()) {
            return "";
        }
        return String.join(JOIN_DELIMITER, stringList);
    }

    @Override
    public List<String> convertToEntityAttribute(String string) {
        if (string == null || string.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] tokens = string.split(SPLIT_REGEX);
        List<String> items = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }
}

