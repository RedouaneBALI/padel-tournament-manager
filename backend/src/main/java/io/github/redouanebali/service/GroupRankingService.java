package io.github.redouanebali.service;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Group;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.TeamSide;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupRankingService {

  public static List<PlayerPair> computeRanking(Group group, List<Game> allGamesOfRound) {
    class SetStats {

      int points    = 0;
      int gamesWon  = 0;
      int gamesLost = 0;
    }

    Map<PlayerPair, SetStats> statsMap = new HashMap<>();

    for (PlayerPair pair : group.getPairs()) {
      statsMap.put(pair, new SetStats());
    }

    for (Game game : allGamesOfRound) {
      PlayerPair teamA = game.getTeamA();
      PlayerPair teamB = game.getTeamB();

      if (group.getPairs().contains(teamA) && group.getPairs().contains(teamB) && game.isFinished()) {
        TeamSide winner = game.getWinnerSide();
        if (winner == TeamSide.TEAM_A) {
          statsMap.get(teamA).points += 1;
        } else if (winner == TeamSide.TEAM_B) {
          statsMap.get(teamB).points += 1;
        }

        int numSets = game.getScore().getSets().size();
        for (int i = 0; i < numSets; i++) {
          int teamAScore = game.getScore().getSets().get(i).getTeamAScore();
          int teamBScore = game.getScore().getSets().get(i).getTeamBScore();

          statsMap.get(teamA).gamesWon += teamAScore;
          statsMap.get(teamA).gamesLost += teamBScore;
          statsMap.get(teamB).gamesWon += teamBScore;
          statsMap.get(teamB).gamesLost += teamAScore;
        }
      }
    }

    return statsMap.entrySet().stream()
                   .sorted((e1, e2) -> {
                     int cmp = Integer.compare(e2.getValue().points, e1.getValue().points);
                     if (cmp != 0) {
                       return cmp;
                     }
                     int diff1 = e1.getValue().gamesWon - e1.getValue().gamesLost;
                     int diff2 = e2.getValue().gamesWon - e2.getValue().gamesLost;
                     return Integer.compare(diff2, diff1);
                   })
                   .map(Map.Entry::getKey)
                   .toList();
  }
}
