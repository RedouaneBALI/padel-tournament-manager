package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for winner propagation between rounds, especially for qualifiers.
 */
class WinnerPropagationTest {

  @Test
  void testQualifierPropagation_winnersPlacedInCorrectOrder() {
    // Given: A tournament with Q2 (4 matches) propagating to R32 (4 QUALIFIER slots)
    // Config: preQual=16, nbQualifiers=4, mainDraw=32
    // There should be 28 teams: 12 go directly to R32, 16 go through Q1
    Tournament tournament = TestFixtures.makeTournament(16, 4, 32, 8, 4, io.github.redouanebali.model.format.DrawMode.SEEDED);

    // Create 28 teams ALL with seeds (1-28)
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(28);
    // Teams are already created with seeds 1, 2, 3, ..., 28

    // Generate the draw
    TournamentBuilder.setupAndPopulateTournament(tournament, teams);

    // Get Q1, Q2 and R32
    Round q1Round  = tournament.getRoundByStage(Stage.Q1);
    Round q2Round  = tournament.getRoundByStage(Stage.Q2);
    Round r32Round = tournament.getRoundByStage(Stage.R32);

    assertNotNull(q1Round, "Q1 must exist");
    assertNotNull(q2Round, "Q2 must exist");
    assertNotNull(r32Round, "R32 must exist");

    // DEBUG: Display Q1 content
    long teamsInQ1 = q1Round.getGames().stream()
                            .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                            .filter(p -> p != null && !p.isBye() && !p.isQualifier())
                            .count();

    // Check that Q1 contains teams
    assertTrue(teamsInQ1 > 0, "Q1 must contain teams after draw generation");

    // Simulate Q1 results (8 matches → 8 winners to Q2)
    // IMPORTANT: Use TestFixtures.createScoreWithWinner() to create valid scores
    for (Game game : q1Round.getGames()) {
      PlayerPair winner = null;
      if (game.getTeamA() != null && !game.getTeamA().isBye()) {
        winner = game.getTeamA();
      } else if (game.getTeamB() != null && !game.getTeamB().isBye()) {
        winner = game.getTeamB();
      }

      if (winner != null) {
        // Create a valid score with the winner
        game.setScore(TestFixtures.createScoreWithWinner(game, winner));
      }
    }

    // Propagate Q1 → Q2 (first propagation)
    TournamentBuilder.propagateWinners(tournament);

    long teamsInQ2 = q2Round.getGames().stream()
                            .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                            .filter(p -> p != null && !p.isBye() && !p.isQualifier())
                            .count();

    // Check that Q2 is filled
    assertTrue(teamsInQ2 > 0, "Q2 must have teams after propagation from Q1");

    // Simulate Q2 results and generate winners with valid scores
    List<PlayerPair> q2Winners = new ArrayList<>();
    for (int i = 0; i < q2Round.getGames().size(); i++) {
      Game game = q2Round.getGames().get(i);

      // Use team A as the winner (or B if A is null/BYE)
      PlayerPair winner = null;
      if (game.getTeamA() != null && !game.getTeamA().isBye()) {
        winner = game.getTeamA();
      } else if (game.getTeamB() != null && !game.getTeamB().isBye()) {
        winner = game.getTeamB();
      }

      if (winner != null) {
        // Create a valid score for this match
        game.setScore(TestFixtures.createScoreWithWinner(game, winner));
        q2Winners.add(winner);
      }
    }

    // Collect QUALIFIER slots in R32 BEFORE propagation
    List<QualifierSlotInfo> qualifierSlotsBefore = new ArrayList<>();
    for (int gameIdx = 0; gameIdx < r32Round.getGames().size(); gameIdx++) {
      Game game = r32Round.getGames().get(gameIdx);

      if (game.getTeamA() != null && game.getTeamA().isQualifier()) {
        qualifierSlotsBefore.add(new QualifierSlotInfo(gameIdx, true, "TeamA"));
      }
      if (game.getTeamB() != null && game.getTeamB().isQualifier()) {
        qualifierSlotsBefore.add(new QualifierSlotInfo(gameIdx, false, "TeamB"));
      }
    }

    // When: Propagate Q2 winners to R32 (second propagation)
    TournamentBuilder.propagateWinners(tournament);

    // Then: Check that each Q2 winner is placed in the correct order in R32
    for (int i = 0; i < q2Winners.size() && i < qualifierSlotsBefore.size(); i++) {
      PlayerPair        expectedWinner = q2Winners.get(i);
      QualifierSlotInfo targetSlot     = qualifierSlotsBefore.get(i);

      Game       r32Game    = r32Round.getGames().get(targetSlot.gameIndex);
      PlayerPair actualTeam = targetSlot.isTeamA ? r32Game.getTeamA() : r32Game.getTeamB();

      assertNotNull(actualTeam,
                    String.format("Slot R32[%d].%s must not be null after propagation",
                                  targetSlot.gameIndex, targetSlot.side));

      assertEquals(expectedWinner.getSeed(), actualTeam.getSeed(),
                   String.format("The winner of match Q2-%d (seed %d) must be placed in slot Q%d (R32[%d].%s), " +
                                 "but found seed %d",
                                 i + 1,
                                 expectedWinner.getSeed(),
                                 i + 1,
                                 targetSlot.gameIndex,
                                 targetSlot.side,
                                 actualTeam.getSeed()));

      assertEquals(expectedWinner.getPlayer1().getName(), actualTeam.getPlayer1().getName(),
                   String.format("The winner of match Q2-%d must be correctly placed in slot Q%d",
                                 i + 1, i + 1));
    }
  }

  @Test
  void testQualifierPropagation_outOfOrderExecution() {
    // Given: A tournament with Q2 → R32
    Tournament       tournament = TestFixtures.makeTournament(16, 4, 32, 8, 4, io.github.redouanebali.model.format.DrawMode.SEEDED);
    List<PlayerPair> teams      = TestFixtures.createPlayerPairs(16);
    TournamentBuilder.setupAndPopulateTournament(tournament, teams);

    Round q1Round  = tournament.getRoundByStage(Stage.Q1);
    Round q2Round  = tournament.getRoundByStage(Stage.Q2);
    Round r32Round = tournament.getRoundByStage(Stage.R32);

    // Simulate Q1 and propagate to Q2
    for (Game game : q1Round.getGames()) {
      if (game.getTeamA() != null && !game.getTeamA().isBye()) {
        game.setWinnerSide(TeamSide.TEAM_A);
      }
    }
    TournamentBuilder.propagateWinners(tournament);

    // Create 4 identifiable winners for Q2
    List<PlayerPair> q2Winners = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      PlayerPair winner = new PlayerPair();
      winner.setPlayer1(new Player("Match" + (i + 1)));
      winner.setPlayer2(new Player("Winner" + (i + 1)));
      winner.setSeed(200 + i);
      q2Winners.add(winner);
    }

    // Scenario: Matches finish OUT OF ORDER
    // Match 4 finishes first, then Match 2, then Match 1, then Match 3
    int[] executionOrder = {3, 1, 0, 2}; // Indices of matches finishing in this order

    for (int execIdx = 0; execIdx < executionOrder.length; execIdx++) {
      int        matchIdx = executionOrder[execIdx];
      Game       game     = q2Round.getGames().get(matchIdx);
      PlayerPair winner   = q2Winners.get(matchIdx);
      game.setTeamA(winner);
      game.setTeamB(PlayerPair.bye());
      game.setWinnerSide(TeamSide.TEAM_A);

      // Propagate immediately after each match
      TournamentBuilder.propagateWinners(tournament);
    }

    // Then: Check that winners are STILL in the correct order
    List<QualifierSlotInfo> qualifierSlots = new ArrayList<>();
    for (int gameIdx = 0; gameIdx < r32Round.getGames().size(); gameIdx++) {
      Game game = r32Round.getGames().get(gameIdx);

      if (game.getTeamA() != null && game.getTeamA().getSeed() >= 200 && game.getTeamA().getSeed() < 300) {
        qualifierSlots.add(new QualifierSlotInfo(gameIdx, true, "TeamA"));
      }
      if (game.getTeamB() != null && game.getTeamB().getSeed() >= 200 && game.getTeamB().getSeed() < 300) {
        qualifierSlots.add(new QualifierSlotInfo(gameIdx, false, "TeamB"));
      }
    }

    for (int i = 0; i < qualifierSlots.size(); i++) {
      QualifierSlotInfo slot       = qualifierSlots.get(i);
      Game              r32Game    = r32Round.getGames().get(slot.gameIndex);
      PlayerPair        actualTeam = slot.isTeamA ? r32Game.getTeamA() : r32Game.getTeamB();

      int expectedMatchIndex = i; // Slot Q1 must contain the winner of match Q2-1, etc.
      int actualMatchIndex   = actualTeam.getSeed() - 200;

      assertEquals(expectedMatchIndex, actualMatchIndex,
                   String.format("Slot Q%d must contain the winner of match Q2-%d, but contains the winner of match Q2-%d",
                                 i + 1, expectedMatchIndex + 1, actualMatchIndex + 1));
    }
  }

  @Test
  void testWinnerModificationAndCancellation() {
    // Given: A simple tournament with R32 and R16
    Tournament tournament = TestFixtures.makeTournament(0, 0, 32, 0, 0, DrawMode.SEEDED);

    List<PlayerPair> teams = TestFixtures.createPlayerPairs(32);
    TournamentBuilder.setupAndPopulateTournament(tournament, teams);

    Round r32Round = tournament.getRoundByStage(Stage.R32);
    Round r16Round = tournament.getRoundByStage(Stage.R16);

    assertNotNull(r32Round);
    assertNotNull(r16Round);

    // Take the first match of R32
    Game       game  = r32Round.getGames().get(0);
    PlayerPair teamA = game.getTeamA();
    PlayerPair teamB = game.getTeamB();

    assertNotNull(teamA);
    assertNotNull(teamB);

    // Set the winner as teamA
    game.setScore(TestFixtures.createScoreWithWinner(game, teamA));
    TournamentBuilder.propagateWinners(tournament);

    // Check that the winner is propagated to R16
    Game       nextGame   = r16Round.getGames().get(0);
    PlayerPair propagated = nextGame.getTeamA();
    assertEquals(teamA, propagated, "The winner teamA must be propagated to R16");

    // Change the winner to teamB
    game.setScore(TestFixtures.createScoreWithWinner(game, teamB));
    TournamentBuilder.propagateWinners(tournament);

    // Check that now it's teamB
    propagated = nextGame.getTeamA();
    assertEquals(teamB, propagated, "The modified winner must be teamB in R16");

    // Cancel the victory
    game.setScore(null);
    TournamentBuilder.propagateWinners(tournament);

    propagated = nextGame.getTeamA();
    assertNull(propagated, "After cancellation, the slot in R16 must be reset");

    // Intermediate score: unfinished match, no winner
    game.setScore(new Score()); // empty score, no winner
    TournamentBuilder.propagateWinners(tournament);

    // Check that no team is propagated
    propagated = nextGame.getTeamA();
    assertNull(propagated, "No team should be propagated until there is a winner");
  }

  /**
   * Helper class to store info about a QUALIFIER slot
   */
  private record QualifierSlotInfo(int gameIndex, boolean isTeamA, String side) {

  }
}
