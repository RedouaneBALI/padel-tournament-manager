package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PlayerPair {

  private Player player1;
  private Player player2;
  private int    seed;

  @Override
  public String toString() {
    return player1 + " - " + player2 + " : " + seed;
  }
}
