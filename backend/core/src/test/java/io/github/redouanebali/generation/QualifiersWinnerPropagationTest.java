package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.util.TestFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test for QUALIFS_KO mode: - Create tournament with qualification phase + main draw - Set winner in qualification match - Verify
 * propagation to main draw - Modify score (change winner) - Verify replacement in main draw
 */
class QualifiersWinnerPropagationTest {

  /**
   * Test scenario: - 4 teams in qualification (2 matches for 2 qualifiers) - 6 direct entries in main draw (8 slots total with 2 qualifiers) - Set
   * winner in Q1 match 1 (team A wins) - Verify team A propagates as Q1 in main draw - Modify Q1 match 1 score (team B wins instead) - Verify team B
   * replaces team A as Q1 in main draw
   */
  @Test
  void testQualifierWinnerPropagation_andReplaceOnScoreModification() {
    // ============= SETUP: Create tournament with QUALIFS_KO =============
    // Tournament: 4 teams in qualif (Q1 = 2 matches for 2 qualifiers) + 8 slots main draw (6 direct + 2 qualifiers)
    Tournament tournament = TestFixtures.makeTournament(
        4,              // preQualDrawSize: 4 teams in qualification
        2,              // nbQualifiers: 2 teams qualify
        8,              // mainDrawSize: 8 slots in main draw
        6,              // nbSeedsMain: 6 direct entries to main draw
        0,              // nbSeedsQualify: 0 seeds in qualifications
        DrawMode.MANUAL
    );

    // Create 10 teams: 4 for qualif + 6 for direct main draw
    List<PlayerPair> allTeams    = TestFixtures.createPlayerPairs(10);
    List<PlayerPair> qualifTeams = allTeams.subList(0, 4);    // Teams 1-4 for qualification
    List<PlayerPair> directTeams = allTeams.subList(4, 10);   // Teams 5-10 for direct main draw

    // Set seeds for direct teams
    for (int i = 0; i < directTeams.size(); i++) {
      directTeams.get(i).setSeed(i + 1); // Seeds 1-6 for direct entries
    }

    // Initialize empty rounds structure
    TournamentBuilder.initializeEmptyRounds(tournament);

    // ============= PLACE TEAMS IN QUALIFICATION (Q1) =============
    Round q1Round = tournament.getRoundByStage(Stage.Q1);
    assertNotNull(q1Round, "Q1 round must exist");
    assertEquals(2, q1Round.getGames().size(), "Q1 should have 2 matches for 4 teams");

    // Q1 Match 1: Team 1 vs Team 2
    Game q1Match1 = q1Round.getGames().get(0);
    q1Match1.setId(1001L); // Simulate persisted game with ID
    q1Match1.setTeamA(qualifTeams.get(0)); // Team 1
    q1Match1.setTeamB(qualifTeams.get(1)); // Team 2

    // Q1 Match 2: Team 3 vs Team 4
    Game q1Match2 = q1Round.getGames().get(1);
    q1Match2.setId(1002L); // Simulate persisted game with ID
    q1Match2.setTeamA(qualifTeams.get(2)); // Team 3
    q1Match2.setTeamB(qualifTeams.get(3)); // Team 4

    // ============= PLACE TEAMS IN MAIN DRAW WITH QUALIFIER PLACEHOLDERS =============
    Round mainRound = tournament.getRoundByStage(Stage.QUARTERS);
    assertNotNull(mainRound, "Main draw round (QUARTERS) must exist");
    assertEquals(4, mainRound.getGames().size(), "Main draw should have 4 games");

    // Main draw setup: 6 direct entries (seeds 1-6) + 2 qualifiers (Q1, Q2)
    // Assign IDs to all main draw games to simulate persistence
    mainRound.getGames().get(0).setId(2001L);
    mainRound.getGames().get(1).setId(2002L);
    mainRound.getGames().get(2).setId(2003L);
    mainRound.getGames().get(3).setId(2004L);

    // Games:
    // Game 1: Seed 1 vs Q1
    mainRound.getGames().get(0).setTeamA(directTeams.get(0)); // Seed 1
    mainRound.getGames().get(0).setTeamB(PlayerPair.qualifier(1)); // Q1 placeholder

    // Game 2: Seed 2 vs Q2
    mainRound.getGames().get(1).setTeamA(directTeams.get(1)); // Seed 2
    mainRound.getGames().get(1).setTeamB(PlayerPair.qualifier(2)); // Q2 placeholder

    // Game 3: Seed 3 vs Seed 4
    mainRound.getGames().get(2).setTeamA(directTeams.get(2)); // Seed 3
    mainRound.getGames().get(2).setTeamB(directTeams.get(3)); // Seed 4

    // Game 4: Seed 5 vs Seed 6
    mainRound.getGames().get(3).setTeamA(directTeams.get(4)); // Seed 5
    mainRound.getGames().get(3).setTeamB(directTeams.get(5)); // Seed 6

    // ============= TEST 1: SET WINNER IN Q1 MATCH 1 (Team 1 wins) =============
    // Set score: Team 1 beats Team 2
    q1Match1.setScore(TestFixtures.createScoreWithWinner(q1Match1, qualifTeams.get(0)));

    // Verify Q1 Match 1 is finished and Team 1 is winner
    assertTrue(q1Match1.isFinished(), "Q1 Match 1 should be finished");
    assertEquals(qualifTeams.get(0), q1Match1.getWinner(), "Team 1 should be the winner of Q1 Match 1");

    // Set score for Q1 Match 2 (Team 3 wins, just to have a complete picture)
    q1Match2.setScore(TestFixtures.createScoreWithWinner(q1Match2, qualifTeams.get(2)));

    // Propagate winners from qualifications to main draw
    TournamentBuilder.propagateWinners(tournament);

    // ============= VERIFY: Team 1 propagated to main draw as Q1 =============
    // Main draw Game 1 should now have Team 1 (winner of Q1 Match 1) in place of Q1 placeholder
    PlayerPair actualQ1InMainDraw = mainRound.getGames().get(0).getTeamB();
    assertNotNull(actualQ1InMainDraw, "Q1 position in main draw should be filled");
    assertEquals(qualifTeams.get(0), actualQ1InMainDraw, "Team 1 should now be Q1 in main draw");

    // Verify Team 1 is no longer marked as qualifier (it's now a real team)
    assertFalse(actualQ1InMainDraw.isQualifier(), "Propagated qualifier should no longer be marked as qualifier");

    // ============= TEST 2: MODIFY Q1 MATCH 1 SCORE (Team 2 wins instead) =============
    // Change the winner: Team 2 beats Team 1
    q1Match1.setScore(TestFixtures.createScoreWithWinner(q1Match1, qualifTeams.get(1)));

    // Verify Q1 Match 1 is still finished but Team 2 is now winner
    assertTrue(q1Match1.isFinished(), "Q1 Match 1 should still be finished");
    assertEquals(qualifTeams.get(1), q1Match1.getWinner(), "Team 2 should now be the winner of Q1 Match 1");

    // Propagate winners again (should replace Team 1 with Team 2)
    TournamentBuilder.propagateWinners(tournament);

    // ============= VERIFY: Team 2 replaced Team 1 as Q1 in main draw =============
    PlayerPair updatedQ1InMainDraw = mainRound.getGames().get(0).getTeamB();
    assertNotNull(updatedQ1InMainDraw, "Q1 position in main draw should still be filled");
    assertEquals(qualifTeams.get(1), updatedQ1InMainDraw, "Team 2 should now be Q1 in main draw (replacing Team 1)");

    // Verify Team 2 is not marked as qualifier
    assertFalse(updatedQ1InMainDraw.isQualifier(), "Updated propagated qualifier should not be marked as qualifier");

    // ============= VERIFY Q2 also works correctly =============
    PlayerPair actualQ2InMainDraw = mainRound.getGames().get(1).getTeamB();
    assertNotNull(actualQ2InMainDraw, "Q2 position in main draw should be filled");
    assertEquals(qualifTeams.get(2), actualQ2InMainDraw, "Team 3 should be Q2 in main draw");

    // ============= SANITY CHECKS =============
    // Verify seeds in main draw are unchanged
    assertEquals(directTeams.get(0), mainRound.getGames().get(0).getTeamA(), "Seed 1 should remain unchanged");
    assertEquals(directTeams.get(1), mainRound.getGames().get(1).getTeamA(), "Seed 2 should remain unchanged");
    assertEquals(directTeams.get(2), mainRound.getGames().get(2).getTeamA(), "Seed 3 should remain unchanged");
    assertEquals(directTeams.get(3), mainRound.getGames().get(2).getTeamB(), "Seed 4 should remain unchanged");
    assertEquals(directTeams.get(4), mainRound.getGames().get(3).getTeamA(), "Seed 5 should remain unchanged");
    assertEquals(directTeams.get(5), mainRound.getGames().get(3).getTeamB(), "Seed 6 should remain unchanged");
  }

  /**
   * CRITICAL TEST: Simulates real DB behavior with stable Game IDs.
   *
   * In production: - Games are persisted with stable IDs - Cache keys are based on Game IDs (not identityHashCode) - When tournament is reloaded from
   * DB, Game IDs stay the same - Cache remains valid across multiple API calls
   *
   * This test verifies that qualifier replacement works correctly with stable IDs, which is the real production scenario.
   */
  @Test
  void testQualifierReplacementWithCacheClear_simulatesDBReload() {
    // Setup tournament (same as first test)
    Tournament       tournament  = TestFixtures.makeTournament(4, 2, 8, 6, 0, DrawMode.MANUAL);
    List<PlayerPair> allTeams    = TestFixtures.createPlayerPairs(10);
    List<PlayerPair> qualifTeams = allTeams.subList(0, 4);
    List<PlayerPair> directTeams = allTeams.subList(4, 10);

    for (int i = 0; i < directTeams.size(); i++) {
      directTeams.get(i).setSeed(i + 1);
    }

    TournamentBuilder.initializeEmptyRounds(tournament);

    Round q1Round  = tournament.getRoundByStage(Stage.Q1);
    Game  q1Match1 = q1Round.getGames().get(0);
    q1Match1.setId(3001L); // Simulate persisted game
    q1Match1.setTeamA(qualifTeams.get(0));
    q1Match1.setTeamB(qualifTeams.get(1));

    Game q1Match2 = q1Round.getGames().get(1);
    q1Match2.setId(3002L); // Simulate persisted game
    q1Match2.setTeamA(qualifTeams.get(2));
    q1Match2.setTeamB(qualifTeams.get(3));

    Round mainRound = tournament.getRoundByStage(Stage.QUARTERS);
    mainRound.getGames().get(0).setId(4001L); // Simulate persisted game
    mainRound.getGames().get(0).setTeamA(directTeams.get(0));
    mainRound.getGames().get(0).setTeamB(PlayerPair.qualifier(1));
    mainRound.getGames().get(1).setId(4002L); // Simulate persisted game
    mainRound.getGames().get(1).setTeamA(directTeams.get(1));
    mainRound.getGames().get(1).setTeamB(PlayerPair.qualifier(2));

    // First propagation: Team 1 wins
    q1Match1.setScore(TestFixtures.createScoreWithWinner(q1Match1, qualifTeams.get(0)));
    q1Match2.setScore(TestFixtures.createScoreWithWinner(q1Match2, qualifTeams.get(2)));

    TournamentBuilder.propagateWinners(tournament);

    assertEquals(qualifTeams.get(0), mainRound.getGames().get(0).getTeamB(), "Team 1 should be Q1");

    // Second propagation: Team 2 wins (replace Team 1)
    // With stable Game IDs (like in real DB), cache keys remain valid across calls
    q1Match1.setScore(TestFixtures.createScoreWithWinner(q1Match1, qualifTeams.get(1)));

    TournamentBuilder.propagateWinners(tournament);

    // This assertion verifies the fix works with stable IDs (real DB scenario)
    assertEquals(qualifTeams.get(1), mainRound.getGames().get(0).getTeamB(),
                 "Team 2 should replace Team 1 with stable Game IDs (real DB behavior)");
  }

  /**
   * Additional test: Ensure qualifier replacement doesn't affect other qualifiers or seeded entries
   */
  @Test
  void testMultipleQualifiersIndependence() {
    // Tournament: 4 teams in qualif (2 qualifiers) + 8 slots main draw (6 direct + 2 qualifiers)
    Tournament tournament = TestFixtures.makeTournament(
        4,  // preQualDrawSize
        2,  // nbQualifiers
        8,  // mainDrawSize
        6,  // nbSeedsMain
        0,  // nbSeedsQualify
        DrawMode.MANUAL
    );

    List<PlayerPair> allTeams    = TestFixtures.createPlayerPairs(10);
    List<PlayerPair> qualifTeams = allTeams.subList(0, 4);
    List<PlayerPair> directTeams = allTeams.subList(4, 10);

    for (int i = 0; i < directTeams.size(); i++) {
      directTeams.get(i).setSeed(i + 1);
    }

    TournamentBuilder.initializeEmptyRounds(tournament);

    Round q1Round  = tournament.getRoundByStage(Stage.Q1);
    Game  q1Match1 = q1Round.getGames().get(0);
    q1Match1.setId(5001L); // Simulate persisted game
    q1Match1.setTeamA(qualifTeams.get(0));
    q1Match1.setTeamB(qualifTeams.get(1));

    Game q1Match2 = q1Round.getGames().get(1);
    q1Match2.setId(5002L); // Simulate persisted game
    q1Match2.setTeamA(qualifTeams.get(2));
    q1Match2.setTeamB(qualifTeams.get(3));

    Round mainRound = tournament.getRoundByStage(Stage.QUARTERS);
    mainRound.getGames().get(0).setId(6001L); // Simulate persisted game
    mainRound.getGames().get(0).setTeamA(directTeams.get(0));
    mainRound.getGames().get(0).setTeamB(PlayerPair.qualifier(1));

    mainRound.getGames().get(1).setId(6002L); // Simulate persisted game
    mainRound.getGames().get(1).setTeamA(directTeams.get(1));
    mainRound.getGames().get(1).setTeamB(PlayerPair.qualifier(2));

    // Set both qualifications
    q1Match1.setScore(TestFixtures.createScoreWithWinner(q1Match1, qualifTeams.get(0)));
    q1Match2.setScore(TestFixtures.createScoreWithWinner(q1Match2, qualifTeams.get(2)));

    TournamentBuilder.propagateWinners(tournament);

    // Verify both qualifiers are propagated correctly
    assertEquals(qualifTeams.get(0), mainRound.getGames().get(0).getTeamB(), "Q1 should be Team 1");
    assertEquals(qualifTeams.get(2), mainRound.getGames().get(1).getTeamB(), "Q2 should be Team 3");

    // Modify only Q1 Match 1 (Team 2 wins)
    q1Match1.setScore(TestFixtures.createScoreWithWinner(q1Match1, qualifTeams.get(1)));

    TournamentBuilder.propagateWinners(tournament);

    // Q1 should change to Team 2
    assertEquals(qualifTeams.get(1), mainRound.getGames().get(0).getTeamB(), "Q1 should now be Team 2");

    // Q2 should remain unchanged (Team 3)
    assertEquals(qualifTeams.get(2), mainRound.getGames().get(1).getTeamB(), "Q2 should still be Team 3");
  }
}

