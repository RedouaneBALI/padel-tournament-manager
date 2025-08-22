package io.github.redouanebali.dto.response;

import lombok.Data;

@Data
public class SetScoreDTO {

  private int     teamAScore;
  private int     teamBScore;
  private Integer tieBreakTeamA;
  private Integer tieBreakTeamB;
}
