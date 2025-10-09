package io.github.redouanebali.generation;

import io.github.redouanebali.generation.draw.AutomaticDrawStrategy;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.util.TestFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DebugDrawTest {

  @Test
  void debugOddTeamsCase() {
    // Test case: 27 teams in a 32-draw (5 BYEs expected)
    Tournament tournament = new Tournament();
    tournament.setConfig(TournamentConfig.builder()
                                         .mainDrawSize(32)
                                         .nbSeeds(8)
                                         .format(TournamentFormat.KNOCKOUT)
                                         .nbQualifiers(0)
                                         .build());

    TournamentBuilder.initializeEmptyRounds(tournament);
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(27);

    AutomaticDrawStrategy strategy = new AutomaticDrawStrategy();
    strategy.placePlayers(tournament, teams);

    Round firstRound = tournament.getRounds().get(0);

    System.out.println("=== DRAW WITH 27 TEAMS IN 32-SLOT DRAW (Expected: 5 BYEs) ===");
    System.out.println();

    int gameNum = 1;
    for (Game game : firstRound.getGames()) {
      PlayerPair teamA = game.getTeamA();
      PlayerPair teamB = game.getTeamB();

      String teamAStr = formatTeam(teamA);
      String teamBStr = formatTeam(teamB);

      System.out.printf("Game %2d: %-20s vs %-20s%n", gameNum, teamAStr, teamBStr);
      gameNum++;
    }

    System.out.println();
    System.out.println("=== SUMMARY ===");
    long byeCount = firstRound.getGames().stream()
                              .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                              .filter(team -> team != null && team.isBye())
                              .count();
    System.out.println("Total BYEs: " + byeCount);

    long realTeamCount = firstRound.getGames().stream()
                                   .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                                   .filter(team -> team != null && !team.isBye())
                                   .count();
    System.out.println("Total real teams: " + realTeamCount);

    System.out.println();
    System.out.println("=== TEAMS PLAYING AGAINST BYES ===");
    for (int i = 1; i <= 5; i++) {
      final int seed = i;
      boolean found = firstRound.getGames().stream()
                                .anyMatch(game -> {
                                  PlayerPair teamA = game.getTeamA();
                                  PlayerPair teamB = game.getTeamB();
                                  if (teamA == null || teamB == null) {
                                    return false;
                                  }
                                  return (teamA.getSeed() == seed && teamB.isBye()) ||
                                         (teamB.getSeed() == seed && teamA.isBye());
                                });
      System.out.println("Team #" + seed + " plays against BYE: " + found);
    }
  }

  private String formatTeam(PlayerPair team) {
    if (team == null) {
      return "NULL";
    }
    if (team.isBye()) {
      return "BYE";
    }
    return "Team #" + team.getSeed();
  }
}

