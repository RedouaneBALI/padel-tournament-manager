package io.github.redouanebali.dto.response;

import io.github.redouanebali.model.TeamSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteSummaryDTO {

  private long     teamAVotes;
  private long     teamBVotes;
  private TeamSide currentUserVote;

}

