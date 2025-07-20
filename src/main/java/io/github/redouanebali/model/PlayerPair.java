package io.github.redouanebali.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PlayerPair {

  @ManyToOne
  private Player player1;
  @ManyToOne
  private Player player2;
  private int    seed;

  @Override
  public String toString() {
    return player1 + " - " + player2 + " : " + seed;
  }
}
