package io.github.redouanebali.api.dto.response;

import io.github.redouanebali.model.TeamSide;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateScoreDTO {

  private boolean  tournamentUpdated;
  private TeamSide winner; // TEAM_A, TEAM_B ou null
}

