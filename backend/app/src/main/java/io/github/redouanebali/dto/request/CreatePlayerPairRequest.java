package io.github.redouanebali.dto.request;

import io.github.redouanebali.model.PairType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePlayerPairRequest {

  private String   player1Name;
  private String   player2Name;
  private Integer  seed;
  private PairType type = PairType.NORMAL;

  public CreatePlayerPairRequest(String player1Name, String player2Name) {
    this.player1Name = player1Name;
    this.player2Name = player2Name;
  }

  public CreatePlayerPairRequest(String player1Name, String player2Name, Integer seed) {
    this.player1Name = player1Name;
    this.player2Name = player2Name;
    this.seed        = seed;
  }
}
