package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PoolRankingDetails {

  @Id
  @GeneratedValue
  private Long       id;
  @ManyToOne
  @JoinColumn(name = "player_pair_id")
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
