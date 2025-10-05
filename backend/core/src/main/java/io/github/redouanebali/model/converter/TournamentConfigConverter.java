package io.github.redouanebali.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.model.format.TournamentConfig;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Converter(autoApply = true)
@Slf4j
public class TournamentConfigConverter implements AttributeConverter<TournamentConfig, String> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(TournamentConfig config) {
    if (config == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(config);
    } catch (JsonProcessingException e) {
      log.error("Error converting TournamentConfig to JSON", e);
      return null;
    }
  }

  @Override
  public TournamentConfig convertToEntityAttribute(String json) {
    if (json == null || json.trim().isEmpty()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, TournamentConfig.class);
    } catch (JsonProcessingException e) {
      log.error("Error converting JSON to TournamentConfig: {}", json, e);
      return null;
    }
  }
}
