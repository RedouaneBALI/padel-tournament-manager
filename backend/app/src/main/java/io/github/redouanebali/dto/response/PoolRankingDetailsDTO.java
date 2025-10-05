package io.github.redouanebali.dto.response;

import lombok.Data;

@Data
public class PoolRankingDetailsDTO {

  private Long          pairId;
  private PlayerPairDTO playerPair;
  private int           points;
  private int           setAverage;
}
