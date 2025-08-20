package io.github.redouanebali.model.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TournamentConfigConverter implements AttributeConverter<TournamentConfig, String> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(TournamentConfig attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(attribute);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize TournamentConfig to JSON", e);
    }
  }

  @Override
  public TournamentConfig convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }
    try {
      return MAPPER.readValue(dbData, TournamentConfig.class);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to deserialize TournamentConfig from JSON", e);
    }
  }
}
