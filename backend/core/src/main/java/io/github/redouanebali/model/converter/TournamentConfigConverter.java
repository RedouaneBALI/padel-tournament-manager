package io.github.redouanebali.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.model.format.TournamentConfig;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;

@Converter(autoApply = true)
@Slf4j
public class TournamentConfigConverter implements AttributeConverter<TournamentConfig, Object> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public Object convertToDatabaseColumn(TournamentConfig config) {
    if (config == null) {
      return null;
    }
    try {
      String json = objectMapper.writeValueAsString(config);

      // Try to create PGobject for PostgreSQL
      try {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(json);
        return jsonObject;
      } catch (SQLException | NoClassDefFoundError e) {
        // If PostgreSQL driver not available (H2), return plain String
        return json;
      }
    } catch (JsonProcessingException e) {
      log.error("Error converting TournamentConfig to JSON", e);
      return null;
    }
  }

  @Override
  public TournamentConfig convertToEntityAttribute(Object dbData) {
    if (dbData == null) {
      return null;
    }

    try {
      String json;
      if (dbData instanceof PGobject pgObject) {
        json = pgObject.getValue();
      } else if (dbData instanceof String) {
        json = (String) dbData;
      } else {
        json = dbData.toString();
      }

      if (json == null || json.trim().isEmpty()) {
        return null;
      }

      return objectMapper.readValue(json, TournamentConfig.class);
    } catch (JsonProcessingException e) {
      log.error("Error converting JSON to TournamentConfig: {}", dbData, e);
      return null;
    }
  }
}
