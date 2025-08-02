package io.github.redouanebali.dto;

import io.github.redouanebali.model.TeamSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoreUpdateResponse {

  private boolean  tournamentUpdated;
  private TeamSide winner; // TEAM_A, TEAM_B ou null
}

