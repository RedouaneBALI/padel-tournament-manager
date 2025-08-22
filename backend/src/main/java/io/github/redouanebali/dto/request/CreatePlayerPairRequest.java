package io.github.redouanebali.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreatePlayerPairRequest {

  private String  player1Name;
  private String  player2Name;
  private Integer seed;
}
