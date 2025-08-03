package io.github.redouanebali.dto;

import io.github.redouanebali.model.Score;
import java.time.LocalTime;
import lombok.Data;

@Data
public class GameUpdateRequest {

  private Score     score;
  private LocalTime scheduledTime;
  private String    court;
}
