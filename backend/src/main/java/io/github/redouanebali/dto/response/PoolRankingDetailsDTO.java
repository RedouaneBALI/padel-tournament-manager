package io.github.redouanebali.dto.response;

import lombok.Data;

@Data
public class PoolRankingDetailsDTO {

  private Long   pairId;
  private String pairName;
  private int    points;
  private int    setAverage;
}
