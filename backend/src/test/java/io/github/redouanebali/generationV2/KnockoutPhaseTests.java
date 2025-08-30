package io.github.redouanebali.generationV2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class KnockoutPhaseTests {


  @ParameterizedTest(name = "Tournament with nbPairs={0}, preQualDrawSize={1}, nbQualifiers={2}, mainDrawSize={3}, phaseType={4}")
  @CsvSource({
      // nbPairs, preQualDrawSize, nbQualifiers, mainDrawSize, phaseType, expectedRounds, expectedMatchesPerRound
      // 1. Direct main draw (no qualifiers)
      "32, 0, 0, 32, MAIN_DRAW, R32;R16;QUARTERS;SEMIS;FINAL, 16;8;4;2;1",
      // 2. Direct main draw with 64
      "64, 0, 0, 64, MAIN_DRAW, R64;R32;R16;QUARTERS;SEMIS;FINAL, 32;16;8;4;2;1",
      // 3. Qualification 16->4 feeding main draw 32
      "36, 16, 4, 32, QUALIFS, Q1;Q2, 8;4",
      // 4. Qualification 32->8 feeding main draw 32
      "48, 32, 8, 32, QUALIFS, Q1;Q2, 16;8",
      // 5. Qualification 32->8 feeding main draw 64
      "52, 32, 8, 64, QUALIFS, Q1;Q2, 16;8",
  })
  void testInitializeTournament(int nbPairs,
                                int preQualDrawSize,
                                int nbQualifiers,
                                int mainDrawSize,
                                PhaseType phaseType,
                                String expectedStagesStr,
                                String expectedMatchesStr) {
    // Arrange
    Tournament t = new Tournament();
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .build();
    t.setConfig(cfg);

    KnockoutPhase phase = new KnockoutPhase(
        phaseType == PhaseType.MAIN_DRAW ? mainDrawSize : preQualDrawSize,
        0, // nbSeeds not important
        phaseType,
        DrawMode.SEEDED
    );

    // Act
    Tournament initialized = phase.initialize(t);

    // Assert
    List<String> expectedStages = Arrays.asList(expectedStagesStr.split(";"));
    List<String> actualStages = initialized.getRounds().stream()
                                           .map(r -> r.getStage().name())
                                           .toList();

    assertEquals(expectedStages, actualStages,
                 "Rounds mismatch for config nbPairs=" + nbPairs);

    List<Integer> expectedMatches = Arrays.stream(expectedMatchesStr.split(";"))
                                          .map(String::trim)
                                          .map(Integer::parseInt)
                                          .toList();

    List<Integer> actualMatches = initialized.getRounds().stream()
                                             .map(r -> r.getGames() == null ? 0 : r.getGames().size())
                                             .toList();

    assertEquals(expectedMatches, actualMatches,
                 "Matches per round mismatch for config nbPairs=" + nbPairs + ", stages=" + expectedStagesStr);
  }


  @ParameterizedTest(name = "nbTeams={0}, nbSeeds={1}")
  @CsvSource({
      // nbTeams, nbSeeds, expectedSeedIndices (semicolon-separated)
      "8, 4, 0;7;4;3",
      "16, 8, 0;15;8;7;4;11;12;3",
      "16, 4, 0;15;8;7",
      "32, 16, 0;31;16;15;8;23;24;7;4;27;20;11;12;19;28;3",
      "32, 8, 0;31;16;15;8;23;24;7",
      "64, 16, 0;63;32;31;16;47;48;15;8;55;40;23;24;39;56;7"
  })
  void testBracketSeedPositionsCsv(int nbTeams, int nbSeeds, String expectedIndicesStr) {
    // Build pairs and sort by seed (smallest seed first)
    List<PlayerPair> pairs = TestFixtures.createPairs(nbTeams);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    // here we test seed slots on a full knockout of size nbTeams
    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        nbSeeds,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    List<Integer> seedPositions = knockoutPhase.getSeedsPositions(nbTeams, nbSeeds);

    List<Integer> expectedSeedIndices = Stream.of(expectedIndicesStr.split(";"))
                                              .map(String::trim)
                                              .map(Integer::parseInt)
                                              .toList();

    for (int i = 0; i < expectedSeedIndices.size(); i++) {
      int expectedIdx = expectedSeedIndices.get(i);
      int actualIdx   = seedPositions.get(i);
      assertEquals(expectedIdx, actualIdx,
                   "Seed " + (i + 1) + " expected at index " + expectedIdx + " but was at index " + actualIdx);
    }
  }

  /**
   * This test complements testBracketSeedPositionsCsv by validating the mapping of seeds into Game objects, rather than verifying the seeding indices
   * themselves.
   */
  @ParameterizedTest(name = "placeSeedTeams mapping nbTeams={0}, nbSeeds={1}")
  @CsvSource({
      "64, 32",
      "64, 16",
      "32, 16",
      "32, 8",
      "16, 8",
      "16, 4"
  })
  void testPlaceSeedTeamsMapping(int nbTeams, int nbSeeds) {
    // Arrange: build empty games (nbTeams/2 matches)
    List<Game> games = new ArrayList<>(nbTeams / 2);
    for (int i = 0; i < nbTeams / 2; i++) {
      games.add(new Game());
    }

    // Build pairs sorted by ascending seed
    List<PlayerPair> pairs = TestFixtures.createPairs(nbTeams);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        nbSeeds,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    // Act: place seeds into the game list
    knockoutPhase.placeSeedTeams(games, pairs, nbSeeds);

    // Oracle: use getSeedsPositions for the slot indices, then check the mapping slot -> (game, side)
    List<Integer> seedSlots = knockoutPhase.getSeedsPositions(nbTeams, nbSeeds);

    // Assert that each seeded pair is placed at its exact slot
    for (int i = 0; i < nbSeeds; i++) {
      int     slot      = seedSlots.get(i);
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);

      Game       g            = games.get(gameIndex);
      PlayerPair expectedPair = pairs.get(i); // i-th best seed

      if (left) {
        assertEquals(expectedPair, g.getTeamA(), "Seed " + (i + 1) + " must be on TEAM_A of game " + gameIndex);
      } else {
        assertEquals(expectedPair, g.getTeamB(), "Seed " + (i + 1) + " must be on TEAM_B of game " + gameIndex);
      }
    }

    // Also assert that non-seed slots remain empty after placement
    Set<Integer> seedSet = new HashSet<>(seedSlots);
    for (int slot = 0; slot < nbTeams; slot++) {
      if (seedSet.contains(slot)) {
        continue;
      }
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);
      Game    g         = games.get(gameIndex);
      if (left) {
        assertNull(g.getTeamA(), "Non-seed slot should be empty on TEAM_A of game " + gameIndex);
      } else {
        assertNull(g.getTeamB(), "Non-seed slot should be empty on TEAM_B of game " + gameIndex);
      }
    }
  }


  @ParameterizedTest(name = "getSeedsPositions empty when nbSeeds=0 (nbTeams={0})")
  @CsvSource({
      "8",
      "16",
      "32",
      "64"
  })
  void testGetSeedsPositionsEmptyWhenNoSeeds(int nbTeams) {
    int nbRounds = (int) (Math.log(nbTeams) / Math.log(2));
    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        0,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );
    List<Integer> seedPositions = knockoutPhase.getSeedsPositions(nbTeams, 0);
    assertEquals(0, seedPositions.size(), "Expected no seed positions when nbSeeds=0");
  }

  @ParameterizedTest(name = "placeSeedTeams leaves bracket empty when nbSeeds=0 (nbTeams={0})")
  @CsvSource({
      "8",
      "16",
      "32"
  })
  void testPlaceSeedTeamsDoesNothingWhenNoSeeds(int nbTeams) {
    // Arrange
    List<Game> games = new ArrayList<>(nbTeams / 2);
    for (int i = 0; i < nbTeams / 2; i++) {
      games.add(new Game());
    }
    List<PlayerPair> pairs = TestFixtures.createPairs(nbTeams);

    int nbRounds = (int) (Math.log(nbTeams) / Math.log(2));
    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        0,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    // Act
    knockoutPhase.placeSeedTeams(games, pairs, 0);

    // Assert: every slot remains empty (no seed was placed)
    for (Game g : games) {
      assertNull(g.getTeamA(), "TEAM_A should be empty when nbSeeds=0");
      assertNull(g.getTeamB(), "TEAM_B should be empty when nbSeeds=0");
    }
  }

}