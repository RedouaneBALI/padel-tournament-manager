package io.github.redouanebali.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TournamentFormatConfigConverter implements AttributeConverter<TournamentFormatConfig, String> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(TournamentFormatConfig attribute) {
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to convert TournamentFormatConfig to JSON", e);
    }
  }

  @Override
  public TournamentFormatConfig convertToEntityAttribute(String dbData) {
    try {
      return objectMapper.readValue(dbData, TournamentFormatConfig.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to convert JSON to TournamentFormatConfig", e);
    }
  }
}
