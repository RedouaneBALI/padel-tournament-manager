package io.github.redouanebali.dto.response;

import io.github.redouanebali.model.Stage;
import lombok.Data;

@Data
public class RoundDTO {

  private Long           id;
  private Stage          stage;
  private MatchFormatDTO matchFormat;
}
