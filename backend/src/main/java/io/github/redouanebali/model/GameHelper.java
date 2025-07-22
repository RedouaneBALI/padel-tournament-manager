package io.github.redouanebali.model;

public class GameHelper {

  public static boolean isMatchOver(Score score, MatchFormat format) {
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

    if (format.isSuperTieBreakInFinalSet()
        && teamAWonSets == format.getNumberOfSetsToWin() - 1
        && teamBWonSets == format.getNumberOfSetsToWin() - 1) {
      int superTieA = score.getSets().getLast().getTeamAScore();
      int superTieB = score.getSets().getLast().getTeamBScore();

      return (superTieA >= 10 || superTieB >= 10) && Math.abs(superTieA - superTieB) >= 2;
    }

    return false;
  }


  public static boolean isSetWonBy(int teamScore, int opponentScore, int maxPointsPerSet) {
    if (teamScore < maxPointsPerSet) {
      return false;
    }

    // If both teams reached maxPointsPerSet - 1, play up to maxPointsPerSet + 1
    int tieThreshold = maxPointsPerSet - 1;
    if (opponentScore >= tieThreshold) {
      return teamScore == maxPointsPerSet + 1 && (teamScore - opponentScore) >= 1;
    }

    // Otherwise, 2 points difference needed after reaching maxPointsPerSet
    return (teamScore - opponentScore) >= 2;
  }
}
