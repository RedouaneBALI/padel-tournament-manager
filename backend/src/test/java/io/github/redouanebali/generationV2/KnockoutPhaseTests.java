package io.github.redouanebali.generationV2;

import static io.github.redouanebali.util.TestFixtures.buildEmptyRound;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.util.TestFixtures;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
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
    List<Round> initialized = phase.initialize(t);

    // Assert
    List<String> expectedStages = Arrays.asList(expectedStagesStr.split(";"));
    List<String> actualStages = initialized.stream()
                                           .map(r -> r.getStage().name())
                                           .toList();

    assertEquals(expectedStages, actualStages,
                 "Rounds mismatch for config nbPairs=" + nbPairs);

    List<Integer> expectedMatches = Arrays.stream(expectedMatchesStr.split(";"))
                                          .map(String::trim)
                                          .map(Integer::parseInt)
                                          .toList();

    List<Integer> actualMatches = initialized.stream()
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
    // Arrange: build a round with nbTeams/2 empty games
    Round round = buildEmptyRound(nbTeams);

    // Build pairs sorted by ascending seed
    List<PlayerPair> pairs = TestFixtures.createPairs(nbTeams);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        nbSeeds,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    // Act: place seeds into the round
    knockoutPhase.placeSeedTeams(round, pairs, nbSeeds);

    // Oracle: use getSeedsPositions for the slot indices, then check the mapping slot -> (game, side)
    List<Integer> seedSlots = knockoutPhase.getSeedsPositions(nbTeams, nbSeeds);

    // Assert that each seeded pair is placed at its exact slot
    for (int i = 0; i < nbSeeds; i++) {
      int     slot      = seedSlots.get(i);
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);

      Game       g            = round.getGames().get(gameIndex);
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
      Game    g         = round.getGames().get(gameIndex);
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
    Round            round = buildEmptyRound(nbTeams);
    List<PlayerPair> pairs = TestFixtures.createPairs(nbTeams);

    int nbRounds = (int) (Math.log(nbTeams) / Math.log(2));
    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        0,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    // Act
    knockoutPhase.placeSeedTeams(round, pairs, 0);

    // Assert: every slot remains empty (no seed was placed)
    for (Game g : round.getGames()) {
      assertNull(g.getTeamA(), "TEAM_A should be empty when nbSeeds=0");
      assertNull(g.getTeamB(), "TEAM_B should be empty when nbSeeds=0");
    }
  }


  @ParameterizedTest(name = "placeByeTeams AUTO: drawSize={0}, nbSeeds={1}, totalPairs={2}")
  @CsvSource({
      // drawSize, nbSeeds, totalPairs
      "32, 16, 24",
      "32, 8, 20"
  })
  void testPlaceByeTeams_AutoSeeds(int drawSize, int nbSeeds, int totalPairs) {
    // Arrange: build empty round and create seeded pairs
    Round            round = buildEmptyRound(drawSize);
    List<PlayerPair> pairs = TestFixtures.createPairs(drawSize);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    KnockoutPhase phase = new KnockoutPhase(drawSize, nbSeeds, PhaseType.MAIN_DRAW, DrawMode.SEEDED);

    // Place seeds automatically (auto mode)
    phase.placeSeedTeams(round, pairs, nbSeeds);

    // Act: place BYEs based on the number of registered pairs
    phase.placeByeTeams(round, totalPairs, drawSize, nbSeeds);

    // Assert: BYE count
    int expectedByes = drawSize - totalPairs;
    long actualByes = round.getGames().stream()
                           .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                           .filter(p -> p != null && p.isBye())
                           .count();
    assertEquals(expectedByes, actualByes, "Unexpected BYE count in AUTO mode");

    // BYEs should face the top seeds, in order, until BYEs are exhausted
    List<Integer> seedSlots = phase.getSeedsPositions(drawSize, nbSeeds);
    for (int i = 0; i < Math.min(expectedByes, seedSlots.size()); i++) {
      int     slot      = seedSlots.get(i);
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);
      Game    g         = round.getGames().get(gameIndex);
      if (left) {
        assertTrue(g.getTeamB() != null && g.getTeamB().isBye(), "BYE must face seed#" + (i + 1));
      } else {
        assertTrue(g.getTeamA() != null && g.getTeamA().isBye(), "BYE must face seed#" + (i + 1));
      }
    }
  }

  @ParameterizedTest(name = "placeByeTeams MANUAL(declared seeds, none placed): drawSize={0}, nbSeeds={1}, totalPairs={2}")
  @CsvSource({
      // drawSize, nbSeeds, totalPairs
      "32, 16, 24",  // 8 BYEs opposite top 8 seed slots
      "64, 16, 40",  // 24 BYEs; start opposite seeds then remaining distribution
      "64, 32, 48"   // 16 BYEs opposite 32 seed slots (first 16 covered)
  })
  void testPlaceByeTeams_ManualDeclaredSeedsButNotPlaced(int drawSize, int nbSeeds, int totalPairs) {
    // Arrange: round is empty; admin declared seeds count, but hasn't placed teams yet
    Round         round = buildEmptyRound(drawSize);
    KnockoutPhase phase = new KnockoutPhase(drawSize, nbSeeds, PhaseType.MAIN_DRAW, DrawMode.SEEDED);

    // Act
    phase.placeByeTeams(round, totalPairs, drawSize, nbSeeds);

    // Assert: BYE count
    int expectedByes = drawSize - totalPairs;
    long actualByes = round.getGames().stream()
                           .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                           .filter(p -> p != null && p.isBye())
                           .count();
    assertEquals(expectedByes, actualByes, "Unexpected BYE count in MANUAL declared-seeds mode");

    // BYEs must be opposite to the first min(expectedByes, nbSeeds) theoretical seed slots
    List<Integer> seedSlots = phase.getSeedsPositions(drawSize, nbSeeds);
    for (int i = 0; i < Math.min(expectedByes, seedSlots.size()); i++) {
      int     slot      = seedSlots.get(i);
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);
      Game    g         = round.getGames().get(gameIndex);
      if (left) {
        // seed would be on TEAM_A; BYE must appear on TEAM_B
        assertTrue(g.getTeamB() != null && g.getTeamB().isBye(), "BYE must face seed slot #" + (i + 1));
        // and seed slot remains empty for the admin to fill later
        assertTrue(g.getTeamA() == null || g.getTeamA().isBye(),
                   "Seed slot should be empty or BYE (staggered entries may create BYE vs BYE)");
      } else {
        assertTrue(g.getTeamA() != null && g.getTeamA().isBye(), "BYE must face seed slot #" + (i + 1));
        assertTrue(g.getTeamB() == null || g.getTeamB().isBye(),
                   "Seed slot should be empty or BYE (staggered entries may create BYE vs BYE)");
      }
    }
  }

  /**
   * CSV-driven test for propagateWinners. For each row in src/test/resources/tournament_scenarios.csv, this test: - builds a minimal tournament with
   * the current round (Total pairs slots) and the next round (half as many games) - populates the current round according to the CSV semantics:
   * DefaultQualif -> create Team vs BYE (auto-qualify) Matches      -> create TeamA vs TeamB and set an explicit winner (TeamA) via a score BYE
   *    -> assign remaining BYE entries as BYE vs BYE so they do not produce a winner - calls KnockoutPhase.propagateWinners(t) - asserts that the
   * number of non-null teams in the next round equals (Matches + DefaultQualif) FINAL rows are ignored (no subsequent round). Stage is resolved via
   * Stage.valueOf(roundName.toUpperCase()).
   */
  @ParameterizedTest(name = "CSV propagateWinners: {0} – round={7}")
  @CsvFileSource(resources = "/tournament_scenarios.csv", numLinesToSkip = 1)
  void testPropagateWinners_FromCsvRow(String tournamentId,
                                       int nbPlayerPairs,
                                       int preQualDrawSize,
                                       int nbQualifiers,
                                       int mainDrawSize,
                                       int nbSeeds,
                                       boolean staggeredEntry,
                                       String roundName,
                                       String qualifFrom,
                                       int fromPreviousRound,
                                       int newTeams,
                                       int bye,
                                       int defaultQualif,
                                       int totalPairs,
                                       int pairsNonBye,
                                       int pairsPlaying,
                                       int matches) {
    // Skip FINAL rows using Stage enum: no next round to receive propagation
    Stage currentStageEnum = Stage.valueOf(roundName.toUpperCase());
    if (currentStageEnum == Stage.FINAL) {
      return; // No next round after FINAL, nothing to propagate
    }
    // Build Tournament and attach config using the first 6 parameters
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(nbSeeds)
                                                       .build();
    Tournament t = new Tournament();
    t.setConfig(cfg);

    // Arrange: build a minimal tournament with a single current round and its next round
    Round currentRound = buildEmptyRound(totalPairs); // totalPairs is the draw size for this row
    currentRound.setStage(currentStageEnum);

    // Next round has half as many games in a standard knockout
    Round nextRound = buildEmptyRound(totalPairs / 2);
    t.getRounds().add(currentRound);
    t.getRounds().add(nextRound);

    // Fill current round according to the CSV semantics
    // 1) DefaultQualif: Team vs BYE (auto-qualify)
    int placedDefaults = 0;
    int gameIndex      = 0;
    while (placedDefaults < defaultQualif && gameIndex < currentRound.getGames().size()) {
      Game g = currentRound.getGames().get(gameIndex++);
      if (g.getTeamA() == null && g.getTeamB() == null) {
        g.setTeamA(TestFixtures.buildPairWithSeed(1000 + placedDefaults));
        g.setTeamB(PlayerPair.bye());
        placedDefaults++;
      }
    }

    // 2) Played matches: A vs B with an explicit winner
    int placedPlayed = 0;
    while (placedPlayed < matches && gameIndex < currentRound.getGames().size()) {
      Game g = currentRound.getGames().get(gameIndex++);
      if (g.getTeamA() == null && g.getTeamB() == null) {
        PlayerPair A = TestFixtures.buildPairWithSeed(2000 + placedPlayed * 2);
        PlayerPair B = TestFixtures.buildPairWithSeed(2000 + placedPlayed * 2 + 1);
        g.setTeamA(A);
        g.setTeamB(B);
        g.setFormat(TestFixtures.createSimpleFormat(1));
        g.setScore(TestFixtures.createScoreWithWinner(g, A)); // TEAM_A always wins
        placedPlayed++;
      }
    }

    // 3) Remaining BYEs (if any): fill as BYE vs BYE so they do not produce winners
    int usedByes      = defaultQualif; // one BYE consumed per defaultQualif game
    int remainingByes = Math.max(0, bye - usedByes);
    gameIndex = 0;
    while (remainingByes > 1 && gameIndex < currentRound.getGames().size()) {
      Game g = currentRound.getGames().get(gameIndex++);
      if (g.getTeamA() == null && g.getTeamB() == null) {
        g.setTeamA(PlayerPair.bye());
        g.setTeamB(PlayerPair.bye());
        remainingByes -= 2;
      }
    }
    if (remainingByes == 1) {
      // Put the last odd BYE on one side if needed (won't generate a winner until an opponent arrives)
      for (Game g : currentRound.getGames()) {
        if (g.getTeamA() == null && g.getTeamB() == null) {
          g.setTeamA(PlayerPair.bye());
          break;
        }
      }
    }

    // Act
    KnockoutPhase phase = new KnockoutPhase(totalPairs, nbSeeds, PhaseType.MAIN_DRAW, DrawMode.SEEDED);
    phase.propagateWinners(t);

    // Assert: number of teams propagated to next round equals matches + defaultQualif
    int expectedWinners = matches + defaultQualif;
    long actualNonNullTeams = nextRound.getGames().stream()
                                       .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                       .filter(Objects::nonNull)
                                       .count();

    assertEquals(expectedWinners, actualNonNullTeams,
                 "Propagation mismatch for " + tournamentId + " at round " + roundName);
  }
}