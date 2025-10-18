package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for AutomaticDrawStrategy to verify correct behavior based on tournament format.
 */
class AutomaticDrawStrategyTest {

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
    Round firstRound = tournament.getRounds().getFirst();
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
    Round firstRound = tournament.getRounds().getFirst();

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

  @ParameterizedTest(name = "Draw {0} with {1} teams, {2} seeds - expects {3} BYEs")
  @CsvSource({
      // mainDrawSize, nbPairs, nbSeeds, expectedByes, description
      "64, 48, 16, 16, '64-draw with 48 teams - 16 BYEs'",
      "64, 40, 16, 24, '64-draw with 40 teams - 24 BYEs'",
      "32, 28,  8,  4, '32-draw with 28 teams - 4 BYEs'",
      "32, 24,  8,  8, '32-draw with 24 teams - 8 BYEs'",
      "32, 32,  8,  0, '32-draw full with 32 teams - no BYEs'",
      "16, 12,  4,  4, '16-draw with 12 teams - 4 BYEs'",
      "16, 16,  4,  0, '16-draw full with 16 teams - no BYEs'",
      " 8,  6,  4,  2, '8-draw with 6 teams - 2 BYEs'",
      " 8,  8,  4,  0, '8-draw full with 8 teams - no BYEs'",
      "64, 47, 16, 17, '64-draw with 47 teams (ODD) - 17 BYEs'",
      "64, 41, 16, 23, '64-draw with 41 teams (ODD) - 23 BYEs'",
      "64, 33, 16, 31, '64-draw with 33 teams (ODD) - 31 BYEs'",
      "32, 27,  8,  5, '32-draw with 27 teams (ODD) - 5 BYEs'",
      "32, 25,  8,  7, '32-draw with 25 teams (ODD) - 7 BYEs'",
      "32, 17,  8, 15, '32-draw with 17 teams (ODD) - 15 BYEs'",
      "16, 11,  4,  5, '16-draw with 11 teams (ODD) - 5 BYEs'",
      "16,  9,  4,  7, '16-draw with 9 teams (ODD) - 7 BYEs'"
  })
  void testKnockoutMode_standardTennisPadelLogic(int mainDrawSize,
                                                 int nbPairs,
                                                 int nbSeeds,
                                                 int expectedByes,
                                                 String description) {
    // Given: A tournament in KNOCKOUT mode with specified configuration
    Tournament tournament = new Tournament();
    tournament.setConfig(TournamentConfig.builder()
                                         .mainDrawSize(mainDrawSize)
                                         .nbSeeds(nbSeeds)
                                         .format(TournamentFormat.KNOCKOUT)
                                         .nbQualifiers(0)
                                         .build());

    TournamentBuilder.initializeEmptyRounds(tournament);

    // Create teams
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(nbPairs);

    // When: Apply automatic draw strategy
    AutomaticDrawStrategy strategy = new AutomaticDrawStrategy();
    strategy.placePlayers(tournament, teams);

    // Then: Verify the first round
    Round firstRound = tournament.getRounds().getFirst();

    // Count total BYEs in the draw
    long totalByes = firstRound.getGames().stream()
                               .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                               .filter(team -> team != null && team.isBye())
                               .count();
    assertEquals(expectedByes, totalByes,
                 String.format("Should have exactly %d BYEs for %d teams in a %d-slot draw",
                               expectedByes, nbPairs, mainDrawSize));

    // Verify that the TOP N teams (where N = expectedByes) play against a BYE
    // This is standard tennis/padel logic: BYEs are placed opposite the best N teams
    int nbTeamsWithByes = expectedByes;
    for (int seed = 1; seed <= nbTeamsWithByes; seed++) {
      final int currentSeed = seed;
      boolean seedFoundPlayingBye = firstRound.getGames().stream()
                                              .anyMatch(game -> {
                                                PlayerPair teamA = game.getTeamA();
                                                PlayerPair teamB = game.getTeamB();

                                                if (teamA == null || teamB == null) {
                                                  return false;
                                                }

                                                return (teamA.getSeed() == currentSeed && teamB.isBye()) ||
                                                       (teamB.getSeed() == currentSeed && teamA.isBye());
                                              });

      assertTrue(seedFoundPlayingBye,
                 String.format("Team #%d must play against a BYE (standard tennis/padel logic) - %s",
                               currentSeed, description));
    }

    // Verify that there are NO BYE vs BYE matches
    long byeVsByeMatches = firstRound.getGames().stream()
                                     .filter(g -> g.getTeamA() != null && g.getTeamA().isBye()
                                                  && g.getTeamB() != null && g.getTeamB().isBye())
                                     .count();

    assertEquals(0, byeVsByeMatches,
                 String.format("There must be NO BYE vs BYE matches in the draw - %s", description));

    // Verify that all real teams are placed in the draw
    long realTeamsPlaced = firstRound.getGames().stream()
                                     .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                                     .filter(team -> team != null && !team.isBye() && !team.isQualifier())
                                     .distinct()
                                     .count();

    assertEquals(nbPairs, realTeamsPlaced,
                 String.format("All %d teams should be placed in the draw - %s", nbPairs, description));

    // Additional verification: check that remaining teams play against each other (not against BYEs)
    if (nbPairs > nbTeamsWithByes) {
      int expectedTeamsPlayingEachOther = nbPairs - nbTeamsWithByes;
      int teamsPlayingEachOther         = 0;

      for (Game game : firstRound.getGames()) {
        PlayerPair teamA = game.getTeamA();
        PlayerPair teamB = game.getTeamB();

        if (teamA == null || teamB == null) {
          continue;
        }

        boolean teamAIsLowerRanked = !teamA.isBye() && teamA.getSeed() > nbTeamsWithByes;
        boolean teamBIsLowerRanked = !teamB.isBye() && teamB.getSeed() > nbTeamsWithByes;

        if (teamAIsLowerRanked && teamBIsLowerRanked) {
          teamsPlayingEachOther += 2; // Count both teams
        }
      }

      assertEquals(expectedTeamsPlayingEachOther, teamsPlayingEachOther,
                   String.format("The %d lower-ranked teams (seeds %d-%d) should play against each other - %s",
                                 expectedTeamsPlayingEachOther, nbTeamsWithByes + 1, nbPairs, description));
    }
  }
}
