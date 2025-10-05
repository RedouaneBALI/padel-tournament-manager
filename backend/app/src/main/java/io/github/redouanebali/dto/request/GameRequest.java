package io.github.redouanebali.dto.request;

import lombok.Data;

@Data
public class GameRequest {

  private PlayerPairRequest teamA;
  private PlayerPairRequest teamB;
}
