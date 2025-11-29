package io.github.redouanebali.dto.response;

import io.github.redouanebali.model.TeamSide;
import java.util.List;
import lombok.Data;

@Data
public class ScoreDTO {

  private List<SetScoreDTO> sets;
  private boolean           forfeit;
  private TeamSide          forfeitedBy;
  private String            currentGamePointA;
  private String            currentGamePointB;
  private Integer           tieBreakPointA;
  private Integer           tieBreakPointB;
}
