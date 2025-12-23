package io.github.redouanebali.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.redouanebali.model.TeamSide;
import java.time.LocalTime;
import lombok.Data;

@Data
public class GameSummaryDTO {

  private Long          id;
  private PlayerPairDTO teamA;
  private PlayerPairDTO teamB;
  private boolean       finished;
  private ScoreDTO      score;
  private TeamSide      winnerSide;
  @JsonFormat(pattern = "HH:mm")
  private LocalTime     scheduledTime;
  private String        court;
  private Long          tournamentId;
}
