package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PoolRankingDetails {

  @Id
  @GeneratedValue
  private Long       id;
  @ManyToOne
  private PlayerPair playerPair;
  private int        points; // 1 victory = 1 point
  private int        setAverage; // difference between cumul of points for and points against

  public PoolRankingDetails(PlayerPair playerPair, int points, int setAverage) {
    this.playerPair = playerPair;
    this.points     = points;
    this.setAverage = setAverage;
  }

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
