package io.github.redouanebali.api.dto.request;

import io.github.redouanebali.model.Score;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateGameRequest {

  private Score     score;
  private LocalTime scheduledTime;
  private String    court;
}
