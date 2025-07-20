package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {

  private PlayerPair teamA;
  private PlayerPair teamB;
  private Score      score;

  public boolean isMatchOver(MatchFormat format) {
    int teamAWonSets = 0;
    int teamBWonSets = 0;

    for (SetScore set : score.getSets()) {
      if (isSetWonBy(set.getTeamAScore(), set.getTeamBScore(), format.getPointsPerSet())) {
        teamAWonSets++;
      } else if (isSetWonBy(set.getTeamBScore(), set.getTeamAScore(), format.getPointsPerSet())) {
        teamBWonSets++;
      }
    }

    if (teamAWonSets >= format.getNumberOfSetsToWin() || teamBWonSets >= format.getNumberOfSetsToWin()) {
      return true;
    }

    if (format.isSuperTieBreakInFinalSet()) {
      return score.getSuperTieBreakTeamA() != null && score.getSuperTieBreakTeamB() != null;
    }

    return false;
  }


  private boolean isSetWonBy(int teamScore, int opponentScore, int maxPointsPerSet) {
    if (teamScore >= maxPointsPerSet) {
      int minDifference = (teamScore == maxPointsPerSet && opponentScore == maxPointsPerSet - 1) ? 1 : 2;
      return teamScore - opponentScore >= minDifference;
    }
    return false;
  }
}
