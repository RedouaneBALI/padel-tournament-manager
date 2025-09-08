package io.github.redouanebali.generation;

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

    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        nbSeeds,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    List<Integer> seedPositions = knockoutPhase.getSeedsPositions();
    List<Integer> expectedSeedIndices = Stream.of(expectedIndicesStr.split(";"))
                                              .map(String::trim)
                                              .map(Integer::parseInt)
                                              .toList();

    // Pour TS1 et TS2, on vérifie la position exacte, pour les autres on vérifie l'appartenance à l'ensemble attendu
    for (int i = 0; i < expectedSeedIndices.size(); i++) {
      int expectedIdx = expectedSeedIndices.get(i);
      int actualIdx   = seedPositions.get(i);
      if (i < 2) {
        assertEquals(expectedIdx, actualIdx,
                     "Seed " + (i + 1) + " expected at index " + expectedIdx + " but was at index " + actualIdx);
      } else {
        // Pour TS3+, vérifier que la position est dans l'ensemble des positions possibles du groupe
        List<Integer> possiblePositions = expectedSeedIndices.subList(2, expectedSeedIndices.size());
        assertTrue(possiblePositions.contains(actualIdx),
                   "Seed " + (i + 1) + " (TS3+) at index " + actualIdx + " not in valid group: " + possiblePositions);
      }
    }
  }

  /**
   * This test validates the mapping of seeds into Game objects using the new JSON-based seeding system.
   */
  @ParameterizedTest(name = "placeSeedTeams mapping nbTeams={0}, nbSeeds={1}")
  @CsvSource({
      "64, 16",
      "32, 16",
      "32, 8",
      "16, 8",
      "16, 4"
  })
  void testPlaceSeedTeamsMapping(int nbTeams, int nbSeeds) {
    // Arrange: build a round with nbTeams/2 empty games
    Round            round = TestFixtures.buildEmptyRound(nbTeams);
    List<PlayerPair> pairs = TestFixtures.createPairs(nbTeams);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));
    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        nbSeeds,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );
    // Act: place seeds into the round
    knockoutPhase.placeSeedTeams(round, pairs);
    List<Integer> seedSlots = knockoutPhase.getSeedsPositions();

    // Vérification pour TS1 et TS2 (toujours fixes)
    for (int i = 0; i < Math.min(nbSeeds, 2); i++) {
      int        slot         = seedSlots.get(i);
      int        gameIndex    = slot / 2;
      boolean    left         = (slot % 2 == 0);
      Game       g            = round.getGames().get(gameIndex);
      PlayerPair expectedPair = pairs.get(i);
      if (left) {
        assertEquals(expectedPair, g.getTeamA(), "Seed " + (i + 1) + " (TS" + (i + 1) + ") doit être sur TEAM_A du match " + gameIndex);
      } else {
        assertEquals(expectedPair, g.getTeamB(), "Seed " + (i + 1) + " (TS" + (i + 1) + ") doit être sur TEAM_B du match " + gameIndex);
      }
    }

    // Vérification pour TS3+ (ordre aléatoire, on vérifie juste la présence)
    Set<Integer> slotsTS3Plus = new HashSet<>();
    for (int i = 2; i < nbSeeds; i++) {
      slotsTS3Plus.add(seedSlots.get(i));
    }
    Set<PlayerPair> placedTS3Plus = new HashSet<>();
    for (int slot : slotsTS3Plus) {
      int        gameIndex = slot / 2;
      boolean    left      = (slot % 2 == 0);
      Game       g         = round.getGames().get(gameIndex);
      PlayerPair found     = left ? g.getTeamA() : g.getTeamB();
      assertTrue(found != null && found.getSeed() > 0, "Un seed doit être placé sur le slot " + slot);
      placedTS3Plus.add(found);
    }
    // Vérifie qu'on a bien placé nbSeeds-2 seeds différents pour TS3+
    assertEquals(nbSeeds - 2 < 0 ? 0 : nbSeeds - 2, placedTS3Plus.size(), "Nombre de seeds TS3+ placés incorrect");

    // Vérifie que les autres slots sont vides
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
    List<Integer> seedPositions = knockoutPhase.getSeedsPositions();
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
    Round            round = TestFixtures.buildEmptyRound(nbTeams);
    List<PlayerPair> pairs = TestFixtures.createPairs(nbTeams);

    int nbRounds = (int) (Math.log(nbTeams) / Math.log(2));
    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        0,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    // Act
    knockoutPhase.placeSeedTeams(round, pairs);

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
    Round            round = TestFixtures.buildEmptyRound(drawSize);
    List<PlayerPair> pairs = TestFixtures.createPairs(drawSize);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    KnockoutPhase phase = new KnockoutPhase(drawSize, nbSeeds, PhaseType.MAIN_DRAW, DrawMode.SEEDED);

    // Place seeds automatically (auto mode)
    phase.placeSeedTeams(round, pairs);

    // Act: place BYES based on the number of registered pairs
    phase.placeByeTeams(round, totalPairs);

    // Assert: BYE count
    int expectedByes = drawSize - totalPairs;
    long actualByes = round.getGames().stream()
                           .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                           .filter(p -> p != null && p.isBye())
                           .count();
    assertEquals(expectedByes, actualByes, "Unexpected BYE count in AUTO mode");

    // BYEs should face the top seeds, in order, until BYEs are exhausted
    List<Integer> seedSlots = phase.getSeedsPositions();
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
      "64, 32, 48"   // 16 BYEs opposite 32 seed slots (first 16 couverts)
  })
  void testPlaceByeTeams_ManualDeclaredSeedsButNotPlaced(int drawSize, int nbSeeds, int totalPairs) {
    // Arrange: round is empty; admin declared seeds count, but hasn't placed teams yet
    Round         round = TestFixtures.buildEmptyRound(drawSize);
    KnockoutPhase phase = new KnockoutPhase(drawSize, nbSeeds, PhaseType.MAIN_DRAW, DrawMode.SEEDED);

    // Act
    phase.placeByeTeams(round, totalPairs);

    // Assert: BYE count
    int expectedByes = drawSize - totalPairs;
    long actualByes = round.getGames().stream()
                           .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                           .filter(p -> p != null && p.isBye())
                           .count();
    assertEquals(expectedByes, actualByes, "Unexpected BYE count in MANUAL declared-seeds mode");

    // Vérifie que les BYEs sont placés en face d'autant de seeds que possible
    List<Integer> seedSlots              = phase.getSeedsPositions();
    int           byesPlacedOppositeSeed = 0;
    for (int i = 0; i < seedSlots.size(); i++) {
      int     slot      = seedSlots.get(i);
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);
      Game    g         = round.getGames().get(gameIndex);
      if (left) {
        if (g.getTeamB() != null && g.getTeamB().isBye()) {
          byesPlacedOppositeSeed++;
        }
      } else {
        if (g.getTeamA() != null && g.getTeamA().isBye()) {
          byesPlacedOppositeSeed++;
        }
      }
    }
    // On doit avoir placé au moins min(expectedByes, seedSlots.size()) BYEs en face des seeds
    assertTrue(byesPlacedOppositeSeed >= Math.min(expectedByes, seedSlots.size()),
               "Il doit y avoir au moins "
               + Math.min(expectedByes, seedSlots.size())
               + " BYES en face des seeds, mais il y en a "
               + byesPlacedOppositeSeed);
  }

  /**
   * CSV-driven test for propagateWinners. For each row in src/test/resources/tournament_scenarios.csv, this test: - builds a minimal tournament with
   * the current round (Total pairs slots) and the next round (half as many games) - populates the current round according to the CSV semantics:
   * DefaultQualif -> create Team vs BYE (auto-qualify) Matches      -> create TeamA vs TeamB and set an explicit winner (TeamA) via a score BYE ->
   * assign remaining BYE entries as BYE vs BYE so they do not produce a winner - calls KnockoutPhase.propagateWinners(t) - asserts that the number of
   * non-null teams in the next round equals (Matches + DefaultQualif) FINAL rows are ignored (no subsequent round). Stage is resolved via
   * Stage.valueOf(roundName.toUpperCase()).
   */
  @ParameterizedTest(name = "CSV propagateWinners: {0} – round={8}")
  @CsvFileSource(resources = "/tournament_scenarios.csv", numLinesToSkip = 1)
  void testPropagateWinners_FromCsvRow(String tournamentId,
                                       int nbPlayerPairs,
                                       int preQualDrawSize,
                                       int nbQualifSeeds,
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
    Stage currentStageEnum = Stage.valueOf(roundName.toUpperCase());
    if (currentStageEnum == Stage.FINAL) {
      return;
    }
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(nbSeeds)
                                                       .build();
    Tournament t = new Tournament();
    t.setId(Long.valueOf(tournamentId));
    t.setConfig(cfg);

    KnockoutPhase qualifPhase = null;
    if (preQualDrawSize > 0 && nbQualifiers > 0) {
      qualifPhase = new KnockoutPhase(preQualDrawSize, nbQualifSeeds, PhaseType.QUALIFS, DrawMode.SEEDED);
    }
    KnockoutPhase mainDrawPhase = new KnockoutPhase(mainDrawSize, nbSeeds, PhaseType.MAIN_DRAW, DrawMode.SEEDED);

    // On prépare le round courant
    Round currentRound = TestFixtures.buildEmptyRound(totalPairs);
    currentRound.setStage(currentStageEnum);

    if (nbSeeds > totalPairs) {
      return;
    }

    KnockoutPhase phaseToUse = currentStageEnum.isQualification() ? qualifPhase : mainDrawPhase;
    if (phaseToUse == null) {
      return;
    }

    boolean isFirstRoundOfPhase = false;
    if (currentStageEnum.isQualification()) {
      isFirstRoundOfPhase = currentStageEnum == Stage.Q1;
    } else {
      isFirstRoundOfPhase = currentStageEnum == Stage.fromNbTeams(mainDrawSize);
    }

    if (isFirstRoundOfPhase) {
      // Pour un round initial, on le remplit avec seeds, BYES, équipes restantes et scores
      t.getRounds().clear();
      t.getRounds().add(currentRound);

      List<PlayerPair> allPairs = TestFixtures.createPairs(pairsNonBye);
      phaseToUse.placeSeedTeams(currentRound, allPairs);
      phaseToUse.placeByeTeams(currentRound, pairsNonBye);
      Set<PlayerPair> alreadyPlaced = new HashSet<>();
      for (Game g : currentRound.getGames()) {
        if (g.getTeamA() != null && !g.getTeamA().isBye()) {
          alreadyPlaced.add(g.getTeamA());
        }
        if (g.getTeamB() != null && !g.getTeamB().isBye()) {
          alreadyPlaced.add(g.getTeamB());
        }
      }
      List<PlayerPair> remainingPairs = allPairs.stream()
                                                .filter(p -> !alreadyPlaced.contains(p))
                                                .toList();
      phaseToUse.placeRemainingTeamsRandomly(currentRound, remainingPairs);

      // Simuler les scores pour générer des vainqueurs
      for (Game game : currentRound.getGames()) {
        if (game.getTeamA() != null && game.getTeamB() != null
            && !game.getTeamA().isBye() && !game.getTeamB().isBye()) {
          game.setFormat(TestFixtures.createSimpleFormat(1));
          game.setScore(TestFixtures.createScoreWithWinner(game, game.getTeamA()));
        }
      }

      // Créer et ajouter le nextRound seulement maintenant
      Round nextRound = TestFixtures.buildEmptyRound(totalPairs / 2);
      t.getRounds().add(nextRound);

    } else {
      // Pour un round intermédiaire, créer prevRound avec des équipes et scores
      int   prevRoundSize = totalPairs * 2;
      Round prevRound     = TestFixtures.buildEmptyRound(prevRoundSize);
      prevRound.setStage(Stage.fromNbTeams(prevRoundSize));
      t.getRounds().clear();
      t.getRounds().add(prevRound);
      t.getRounds().add(currentRound);

      // Remplir prevRound avec des équipes et des scores
      List<PlayerPair> prevPairs = TestFixtures.createPairs(prevRoundSize);
      int              idx       = 0;
      for (Game g : prevRound.getGames()) {
        if (idx + 1 < prevPairs.size()) {
          g.setTeamA(prevPairs.get(idx));
          g.setTeamB(prevPairs.get(idx + 1));
          g.setFormat(TestFixtures.createSimpleFormat(1));
          g.setScore(TestFixtures.createScoreWithWinner(g, g.getTeamA()));
          idx += 2;
        }
      }

      // Propager de prevRound vers currentRound
      phaseToUse.propagateWinners(t);

      // Maintenant, simuler les scores dans currentRound pour tester la propagation vers nextRound
      for (Game game : currentRound.getGames()) {
        if (game.getTeamA() != null && game.getTeamB() != null
            && !game.getTeamA().isBye() && !game.getTeamB().isBye()) {
          game.setFormat(TestFixtures.createSimpleFormat(1));
          game.setScore(TestFixtures.createScoreWithWinner(game, game.getTeamA()));
        }
      }

      // Créer et ajouter le nextRound seulement maintenant
      Round nextRound = TestFixtures.buildEmptyRound(totalPairs / 2);
      t.getRounds().add(nextRound);
    }

    // Act : propager les winners du round courant vers le suivant
    phaseToUse.propagateWinners(t);

    // Assert: vérifier la propagation dans le nextRound
    Round nextRound         = t.getRounds().get(t.getRounds().size() - 1);
    int   expectedQualified = matches + defaultQualif;
    long actualNonNullNonByeTeams = nextRound.getGames().stream()
                                             .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                             .filter(Objects::nonNull)
                                             .filter(p -> !p.isBye())
                                             .count();

    assertEquals(expectedQualified, actualNonNullNonByeTeams,
                 "Propagation mismatch for " + tournamentId + " at round " + roundName);
  }

  @ParameterizedTest(name = "nbTeams={0}, nbSeeds={1} - TS1/TS2 fixed positions")
  @CsvSource({
      // nbTeams, nbSeeds, TS1_expected, TS2_expected
      "8, 4, 0, 7",
      "16, 8, 0, 15",
      "16, 4, 0, 15",
      "32, 16, 0, 31",
      "32, 8, 0, 31",
      "64, 16, 0, 63"
  })
  void testSeedPositions_TS1_TS2_Fixed(int nbTeams, int nbSeeds, int expectedTS1, int expectedTS2) {
    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        nbSeeds,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    List<Integer> seedPositions = knockoutPhase.getSeedsPositions();

    // TS1 and TS2 should always be at fixed positions
    assertEquals(expectedTS1, seedPositions.get(0), "TS1 must always be at position " + expectedTS1);
    if (nbSeeds >= 2) {
      assertEquals(expectedTS2, seedPositions.get(1), "TS2 must always be at position " + expectedTS2);
    }
  }

  @ParameterizedTest(name = "nbTeams={0}, nbSeeds={1} - TS3+ random positions")
  @CsvSource({
      // nbTeams, nbSeeds, TS3_possible_positions, TS4_possible_positions
      "8, 4, '3;4', '3;4'",
      "16, 8, '7;8', '7;8'",
      "16, 4, '7;8', '7;8'",
      "32, 8, '15;16', '15;16'",
      "64, 16, '31;32', '31;32'"
  })
  void testSeedPositions_TS3Plus_RandomWithinValidOptions(int nbTeams, int nbSeeds, String ts3Options, String ts4Options) {
    if (nbSeeds < 3) {
      return; // Skip if not enough seeds for TS3+
    }

    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        nbSeeds,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    // Test multiple times to verify randomness
    Set<Integer> observedTS3Positions = new HashSet<>();
    Set<Integer> observedTS4Positions = new HashSet<>();

    for (int i = 0; i < 50; i++) { // Run 50 times to catch randomness
      List<Integer> seedPositions = knockoutPhase.getSeedsPositions();

      if (nbSeeds >= 3) {
        observedTS3Positions.add(seedPositions.get(2));
      }
      if (nbSeeds >= 4) {
        observedTS4Positions.add(seedPositions.get(3));
      }
    }

    // Parse expected positions
    List<Integer> validTS3Positions = Arrays.stream(ts3Options.split(";"))
                                            .map(String::trim)
                                            .map(Integer::parseInt)
                                            .toList();

    List<Integer> validTS4Positions = Arrays.stream(ts4Options.split(";"))
                                            .map(String::trim)
                                            .map(Integer::parseInt)
                                            .toList();

    // Verify TS3 positions are within valid options
    for (int observedPos : observedTS3Positions) {
      assertTrue(validTS3Positions.contains(observedPos),
                 "TS3 position " + observedPos + " not in valid options: " + validTS3Positions);
    }

    // Verify TS4 positions are within valid options
    if (nbSeeds >= 4) {
      for (int observedPos : observedTS4Positions) {
        assertTrue(validTS4Positions.contains(observedPos),
                   "TS4 position " + observedPos + " not in valid options: " + validTS4Positions);
      }
    }

    // Verify that we see some variation (randomness working)
    if (validTS3Positions.size() > 1) {
      assertTrue(observedTS3Positions.size() > 1,
                 "TS3 should show variation across multiple runs, but only saw: " + observedTS3Positions);
    }
  }

  @ParameterizedTest(name = "Comprehensive seed positioning test - nbTeams={0}, nbSeeds={1}")
  @CsvSource({
      "32, 8",
      "64, 16"
  })
  void testSeedPositions_ComprehensiveValidation(int nbTeams, int nbSeeds) {
    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        nbSeeds,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    List<Integer> seedPositions = knockoutPhase.getSeedsPositions();

    // Basic validations
    assertEquals(nbSeeds, seedPositions.size(), "Should return exactly nbSeeds positions");

    // All positions should be unique
    Set<Integer> uniquePositions = new HashSet<>(seedPositions);
    assertEquals(nbSeeds, uniquePositions.size(), "All seed positions should be unique");

    // All positions should be valid (0 <= pos < nbTeams)
    for (int pos : seedPositions) {
      assertTrue(pos >= 0 && pos < nbTeams, "Position " + pos + " should be between 0 and " + (nbTeams - 1));
    }

    // Verify JSON structure is being used correctly by checking known constraints
    assertEquals(0, seedPositions.get(0), "TS1 must always be at position 0");
    assertEquals(nbTeams - 1, seedPositions.get(1), "TS2 must always be at position " + (nbTeams - 1));
  }

  @ParameterizedTest(name = "nbTeams={0}, nbSeeds={1} - Basic seed positioning validation")
  @CsvSource({
      // nbTeams, nbSeeds - Test basic functionality without hardcoded expectations
      "8, 4",
      "16, 8",
      "32, 16",
      "64, 32"
  })
  void testSeedPositions_BasicValidation(int nbTeams, int nbSeeds) {
    KnockoutPhase knockoutPhase = new KnockoutPhase(
        nbTeams,
        nbSeeds,
        PhaseType.MAIN_DRAW,
        DrawMode.SEEDED
    );

    List<Integer> seedPositions = knockoutPhase.getSeedsPositions();

    // Basic validations
    assertEquals(nbSeeds, seedPositions.size(), "Should return exactly nbSeeds positions");

    // All positions should be unique
    Set<Integer> uniquePositions = new HashSet<>(seedPositions);
    assertEquals(nbSeeds, uniquePositions.size(), "All seed positions should be unique");

    // All positions should be valid (0 <= pos < nbTeams)
    for (int pos : seedPositions) {
      assertTrue(pos >= 0 && pos < nbTeams, "Position " + pos + " should be between 0 and " + (nbTeams - 1));
    }
  }
}
