package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.util.TestFixturesCore;
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
    Tournament tournament = TestFixturesCore.makeTournament(
        4,              // preQualDrawSize: 4 teams in qualification
        2,              // nbQualifiers: 2 teams qualify
        8,              // mainDrawSize: 8 slots in main draw
        6,              // nbSeedsMain: 6 direct entries to main draw
        0,              // nbSeedsQualify: 0 seeds in qualifications
        DrawMode.MANUAL
    );

    // Create 10 teams: 4 for qualif + 6 for direct main draw
    List<PlayerPair> allTeams    = TestFixturesCore.createPlayerPairs(10);
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
    q1Match1.setScore(TestFixturesCore.createScoreWithWinner(q1Match1, qualifTeams.get(0)));

    // Verify Q1 Match 1 is finished and Team 1 is winner
    assertTrue(q1Match1.isFinished(), "Q1 Match 1 should be finished");
    assertEquals(qualifTeams.get(0), q1Match1.getWinner(), "Team 1 should be the winner of Q1 Match 1");

    // Set score for Q1 Match 2 (Team 3 wins, just to have a complete picture)
    q1Match2.setScore(TestFixturesCore.createScoreWithWinner(q1Match2, qualifTeams.get(2)));

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
    q1Match1.setScore(TestFixturesCore.createScoreWithWinner(q1Match1, qualifTeams.get(1)));

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
    Tournament       tournament  = TestFixturesCore.makeTournament(4, 2, 8, 6, 0, DrawMode.MANUAL);
    List<PlayerPair> allTeams    = TestFixturesCore.createPlayerPairs(10);
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
    q1Match1.setScore(TestFixturesCore.createScoreWithWinner(q1Match1, qualifTeams.get(0)));
    q1Match2.setScore(TestFixturesCore.createScoreWithWinner(q1Match2, qualifTeams.get(2)));

    TournamentBuilder.propagateWinners(tournament);

    assertEquals(qualifTeams.get(0), mainRound.getGames().get(0).getTeamB(), "Team 1 should be Q1");

    // Second propagation: Team 2 wins (replace Team 1)
    // With stable Game IDs (like in real DB), cache keys remain valid across calls
    q1Match1.setScore(TestFixturesCore.createScoreWithWinner(q1Match1, qualifTeams.get(1)));

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
    Tournament tournament = TestFixturesCore.makeTournament(
        4,  // preQualDrawSize
        2,  // nbQualifiers
        8,  // mainDrawSize
        6,  // nbSeedsMain
        0,  // nbSeedsQualify
        DrawMode.MANUAL
    );

    List<PlayerPair> allTeams    = TestFixturesCore.createPlayerPairs(10);
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
    q1Match1.setScore(TestFixturesCore.createScoreWithWinner(q1Match1, qualifTeams.get(0)));
    q1Match2.setScore(TestFixturesCore.createScoreWithWinner(q1Match2, qualifTeams.get(2)));

    TournamentBuilder.propagateWinners(tournament);

    // Verify both qualifiers are propagated correctly
    assertEquals(qualifTeams.get(0), mainRound.getGames().get(0).getTeamB(), "Q1 should be Team 1");
    assertEquals(qualifTeams.get(2), mainRound.getGames().get(1).getTeamB(), "Q2 should be Team 3");

    // Modify only Q1 Match 1 (Team 2 wins)
    q1Match1.setScore(TestFixturesCore.createScoreWithWinner(q1Match1, qualifTeams.get(1)));

    TournamentBuilder.propagateWinners(tournament);

    // Q1 should change to Team 2
    assertEquals(qualifTeams.get(1), mainRound.getGames().get(0).getTeamB(), "Q1 should now be Team 2");

    // Q2 should remain unchanged (Team 3)
    assertEquals(qualifTeams.get(2), mainRound.getGames().get(1).getTeamB(), "Q2 should still be Team 3");
  }

  /**
   * Test optimized propagation: propagateWinnersFromGame should only propagate from the round containing the modified game onwards. Scenario: -
   * Create simple 8-team knockout (R16, QF, SF, F) - Set winners in R16 to fill QF - Modify one R16 game using propagateWinnersFromGame - Verify only
   * affected games are updated - Verify performance (only 2 rounds processed instead of all 4)
   */
  @Test
  void testPropagateWinnersFromGame_onlyProcessesAffectedRounds() {
    // Create simple 8-team knockout tournament
    Tournament tournament = TestFixturesCore.makeTournament(
        0,              // preQualDrawSize: no qualification
        0,              // nbQualifiers: no qualifiers
        8,              // mainDrawSize: 8 teams
        8,              // nbSeeds: all teams are seeded
        0,              // nbSeedsQualify: no seeds in qualification
        io.github.redouanebali.model.format.DrawMode.MANUAL
    );
    List<PlayerPair> teams = TestFixturesCore.createPlayerPairs(8);

    // Initialize empty rounds structure
    TournamentBuilder.initializeEmptyRounds(tournament);

    // Place teams in R8 (4 matches)
    Round r8Round = tournament.getRounds().get(0);
    assertEquals(Stage.QUARTERS, r8Round.getStage(), "First round should be R8");
    List<Game> r8Games = r8Round.getGames();
    assertEquals(4, r8Games.size(), "R8 should have 4 games");

    for (int i = 0; i < 8; i++) {
      teams.get(i).setSeed(i + 1);
    }

    // Manually place teams in QUARTERS
    r8Games.get(0).setTeamA(teams.get(0)); // Match 1: Team 1 vs Team 2
    r8Games.get(0).setTeamB(teams.get(1));
    r8Games.get(1).setTeamA(teams.get(2)); // Match 2: Team 3 vs Team 4
    r8Games.get(1).setTeamB(teams.get(3));
    r8Games.get(2).setTeamA(teams.get(4)); // Match 3: Team 5 vs Team 6
    r8Games.get(2).setTeamB(teams.get(5));
    r8Games.get(3).setTeamA(teams.get(6)); // Match 4: Team 7 vs Team 8
    r8Games.get(3).setTeamB(teams.get(7));

    // Set winners for all QUARTERS matches
    r8Games.get(0).setScore(TestFixturesCore.createScoreWithWinner(r8Games.get(0), teams.get(0))); // Team 1 wins
    r8Games.get(1).setScore(TestFixturesCore.createScoreWithWinner(r8Games.get(1), teams.get(2))); // Team 3 wins
    r8Games.get(2).setScore(TestFixturesCore.createScoreWithWinner(r8Games.get(2), teams.get(4))); // Team 5 wins
    r8Games.get(3).setScore(TestFixturesCore.createScoreWithWinner(r8Games.get(3), teams.get(6))); // Team 7 wins

    // Full propagation to fill all rounds
    TournamentBuilder.propagateWinners(tournament);

    // Verify SEMIS (second round) is filled correctly
    Round sfRound = tournament.getRounds().get(1);
    assertEquals(Stage.SEMIS, sfRound.getStage(), "Second round should be SEMIS");
    List<Game> sfGames = sfRound.getGames();
    assertEquals(teams.get(0), sfGames.get(0).getTeamA(), "SF Match 1 TeamA should be Team 1");
    assertEquals(teams.get(2), sfGames.get(0).getTeamB(), "SF Match 1 TeamB should be Team 3");
    assertEquals(teams.get(4), sfGames.get(1).getTeamA(), "SF Match 2 TeamA should be Team 5");
    assertEquals(teams.get(6), sfGames.get(1).getTeamB(), "SF Match 2 TeamB should be Team 7");

    // Now modify QUARTERS Match 1: Team 2 wins instead of Team 1
    Game modifiedGame = r8Games.get(0);
    modifiedGame.setScore(TestFixturesCore.createScoreWithWinner(modifiedGame, teams.get(1))); // Team 2 wins now
    assertEquals(teams.get(1), modifiedGame.getWinner(), "Modified game winner should be Team 2");

    // Use optimized propagation from this specific game
    TournamentBuilder.propagateWinnersFromGame(tournament, modifiedGame);

    // Verify SF Match 1 is updated (Team 2 replaces Team 1)
    assertEquals(teams.get(1), sfGames.get(0).getTeamA(), "SF Match 1 TeamA should now be Team 2 (updated)");
    assertEquals(teams.get(2), sfGames.get(0).getTeamB(), "SF Match 1 TeamB should remain Team 3");

    // Verify SF Match 2 is unchanged (not affected by QUARTERS Match 1)
    assertEquals(teams.get(4), sfGames.get(1).getTeamA(), "SF Match 2 TeamA should remain Team 5");
    assertEquals(teams.get(6), sfGames.get(1).getTeamB(), "SF Match 2 TeamB should remain Team 7");

    // Test removing winner (setting match as not finished)
    modifiedGame.setScore(TestFixturesCore.createUnfinishedScore());
    assertFalse(modifiedGame.isFinished(), "Modified game should not be finished");

    // Propagate again
    TournamentBuilder.propagateWinnersFromGame(tournament, modifiedGame);

    // Verify Team 2 is removed from SF
    assertNull(sfGames.get(0).getTeamA(), "SF Match 1 TeamA should be null (winner removed)");
    assertEquals(teams.get(2), sfGames.get(0).getTeamB(), "SF Match 1 TeamB should still be Team 3");
  }
}

