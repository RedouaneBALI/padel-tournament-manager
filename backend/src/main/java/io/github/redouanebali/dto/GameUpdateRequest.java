package io.github.redouanebali.dto;

import io.github.redouanebali.model.Score;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameUpdateRequest {

  private Score     score;
  private LocalTime scheduledTime;
  private String    court;
}
