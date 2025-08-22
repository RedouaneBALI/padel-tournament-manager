package io.github.redouanebali.dto.response;

import lombok.Data;

@Data
public class PlayerPairDTO {

  private Long    id;
  private String  player1Name;
  private String  player2Name;
  private Integer seed;
  private boolean bye;
}
