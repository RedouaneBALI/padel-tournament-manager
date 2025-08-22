package io.github.redouanebali.dto.request;

import io.github.redouanebali.model.Score;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGameRequest {

  private Score     score;
  private LocalTime scheduledTime;
  private String    court;
}
