package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.redouanebali.generation.draw.AutomaticDrawStrategy;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.util.TestFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for AutomaticDrawStrategy to verify correct behavior based on tournament format.
 */
public class AutomaticDrawStrategyTest {

  @Test
  void testKnockoutMode_noQualifiersPlaced() {
    // Given: A tournament in KNOCKOUT mode (not QUALIFS_KO) with 8 teams
    Tournament tournament = new Tournament();
    tournament.setConfig(TournamentConfig.builder()
                                         .mainDrawSize(8)
                                         .nbSeeds(4)
                                         .format(TournamentFormat.KNOCKOUT)  // KNOCKOUT mode, no qualifications
                                         .nbQualifiers(0)  // No qualifiers
                                         .build());

    // Initialize the tournament structure (create empty rounds)
    TournamentBuilder.initializeEmptyRounds(tournament);

    // Create 6 teams for an 8-slot draw
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(6);

    // When: Apply automatic draw strategy
    AutomaticDrawStrategy strategy = new AutomaticDrawStrategy();
    strategy.placePlayers(tournament, teams);

    // Then: No QUALIFIER should be placed in any round
    assertNotNull(tournament.getRounds(), "Tournament should have rounds");

    for (Round round : tournament.getRounds()) {
      for (Game game : round.getGames()) {
        // Check TeamA
        if (game.getTeamA() != null) {
          assertFalse(game.getTeamA().isQualifier(),
                      String.format("TeamA should not be a QUALIFIER in KNOCKOUT mode (Round: %s)", round.getStage()));
        }
        // Check TeamB
        if (game.getTeamB() != null) {
          assertFalse(game.getTeamB().isQualifier(),
                      String.format("TeamB should not be a QUALIFIER in KNOCKOUT mode (Round: %s)", round.getStage()));
        }
      }
    }

    // Verify that BYEs are placed instead (since we have 6 teams in 8 slots)
    Round firstRound = tournament.getRounds().get(0);
    long byeCount = firstRound.getGames().stream()
                              .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                              .filter(team -> team != null && team.isBye())
                              .count();

    assertEquals(2, byeCount, "Should have 2 BYEs for 6 teams in an 8-slot draw");

    // Verify that real teams are placed
    long realTeamCount = firstRound.getGames().stream()
                                   .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                                   .filter(team -> team != null && !team.isBye() && !team.isQualifier())
                                   .count();

    assertEquals(6, realTeamCount, "Should have 6 real teams placed");
  }

  @Test
  void testQualifKOMode_qualifiersArePlaced() {
    // Given: A tournament in QUALIFS_KO mode with qualifications
    Tournament tournament = new Tournament();
    tournament.setConfig(TournamentConfig.builder()
                                         .mainDrawSize(8)
                                         .nbSeeds(4)
                                         .format(TournamentFormat.QUALIF_KO)  // QUALIFS_KO mode
                                         .nbQualifiers(4)  // 4 qualifier slots
                                         .preQualDrawSize(8)  // Q1 has 8 slots
                                         .nbSeedsQualify(2)
                                         .build());

    // Initialize the tournament structure (create empty rounds including Q1)
    TournamentBuilder.initializeEmptyRounds(tournament);

    // Create 8 teams total
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(8);

    // When: Apply automatic draw strategy
    AutomaticDrawStrategy strategy = new AutomaticDrawStrategy();
    strategy.placePlayers(tournament, teams);

    // Then: QUALIFIERs should be placed in the main draw
    assertNotNull(tournament.getRounds(), "Tournament should have rounds");

    // Find the first main draw round (should have qualifiers)
    Round mainDrawRound = tournament.getRounds().stream()
                                    .filter(r -> !r.getStage().isQualification() && r.getStage() != Stage.GROUPS)
                                    .findFirst()
                                    .orElse(null);

    assertNotNull(mainDrawRound, "Should have a main draw round");

    long qualifierCount = mainDrawRound.getGames().stream()
                                       .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                                       .filter(team -> team != null && team.isQualifier())
                                       .count();

    assertEquals(4, qualifierCount,
                 "Should have 4 QUALIFIER placeholders in main draw for QUALIFS_KO mode");

    // Verify that Q1 has real teams (not qualifiers)
    Round q1Round = tournament.getRoundByStage(Stage.Q1);
    assertNotNull(q1Round, "Should have Q1 round");

    long q1QualifierCount = q1Round.getGames().stream()
                                   .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                                   .filter(team -> team != null && team.isQualifier())
                                   .count();

    assertEquals(0, q1QualifierCount, "Q1 should not have QUALIFIER placeholders, only real teams or BYEs");
  }

  @Test
  void testKnockoutMode_allTeamsPlaced() {
    // Given: A tournament in KNOCKOUT mode with exactly 8 teams for 8 slots
    Tournament tournament = new Tournament();
    tournament.setConfig(TournamentConfig.builder()
                                         .mainDrawSize(8)
                                         .nbSeeds(4)
                                         .format(TournamentFormat.KNOCKOUT)
                                         .nbQualifiers(0)
                                         .build());

    TournamentBuilder.initializeEmptyRounds(tournament);

    // Create exactly 8 teams
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(8);

    // When: Apply automatic draw strategy
    AutomaticDrawStrategy strategy = new AutomaticDrawStrategy();
    strategy.placePlayers(tournament, teams);

    // Then: All 8 teams should be placed, no BYEs, no QUALIFIERs
    Round firstRound = tournament.getRounds().get(0);

    long realTeamCount = firstRound.getGames().stream()
                                   .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                                   .filter(team -> team != null && !team.isBye() && !team.isQualifier())
                                   .count();

    assertEquals(8, realTeamCount, "All 8 teams should be placed");

    long byeCount = firstRound.getGames().stream()
                              .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                              .filter(team -> team != null && team.isBye())
                              .count();

    assertEquals(0, byeCount, "Should have no BYEs when draw is full");

    long qualifierCount = firstRound.getGames().stream()
                                    .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                                    .filter(team -> team != null && team.isQualifier())
                                    .count();

    assertEquals(0, qualifierCount, "Should have no QUALIFIERs in KNOCKOUT mode");
  }
}

