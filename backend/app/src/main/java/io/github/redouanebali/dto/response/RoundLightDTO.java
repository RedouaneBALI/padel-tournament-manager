package io.github.redouanebali.dto.response;

import io.github.redouanebali.model.Stage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundLightDTO {

  private Long           id;
  private Stage          stage;
  private MatchFormatDTO matchFormat;
}
