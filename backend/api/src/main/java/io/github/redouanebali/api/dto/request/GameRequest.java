package io.github.redouanebali.api.dto.request;

import lombok.Data;

@Data
public class GameRequest {

  private PlayerPairRequest teamA;
  private PlayerPairRequest teamB;
}
