package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SetScore {
  private int teamAScore;
  private int teamBScore;
  private Integer tieBreakTeamA; // Null si pas de tie-break
  private Integer tieBreakTeamB;
}
