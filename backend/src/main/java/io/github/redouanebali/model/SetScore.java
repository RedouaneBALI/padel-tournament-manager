package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetScore {

  private int     teamAScore;
  private int     teamBScore;
  private Integer tieBreakTeamA; // Null si pas de tie-break
  private Integer tieBreakTeamB;

  public SetScore(int teamAScore, int teamBScore) {
    this.teamAScore = teamAScore;
    this.teamBScore = teamBScore;
  }
}
