package io.github.redouanebali.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Embeddable
@AllArgsConstructor
@Data
public class PoolRankingDetails {

  private PlayerPair playerPair;
  private int        points; // 1 victory = 1 point
  private int        setAverage; // difference between cumul of points for and points against

  @Override
  public String toString() {
    return String.format(
        "%s: %dpt(s), Set Average: %d",
        playerPair != null ? playerPair.toString() : "Unknown Pair",
        points,
        setAverage
    );
  }
}
