package io.github.redouanebali.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePlayerPairRequest {

  private String  player1Name;
  private String  player2Name;
  private Integer seed;

  public CreatePlayerPairRequest(String player1Name, String player2Name) {
    this.player1Name = player1Name;
    this.player2Name = player2Name;
  }
}
