package io.github.redouanebali.dto.response;

import lombok.Data;

@Data
public class GameDTO {

  private Long          id;
  private PlayerPairDTO teamA;
  private PlayerPairDTO teamB;
  private boolean       finished;
  private ScoreDTO      score;
}
