package io.github.redouanebali.generation;

import static io.github.redouanebali.util.TestFixtures.parseInts;
import static io.github.redouanebali.util.TestFixtures.parseStages;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.generation.draw.AutomaticDrawStrategy;
import io.github.redouanebali.generation.draw.DrawStrategy;
import io.github.redouanebali.generation.draw.DrawStrategyFactory;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.util.TestFixtures;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;

class TournamentBuilderTest {

  // --- Small header index helpers shared by provider and test ---
  private static Map<String, Integer> headerIndex;

  // --- Provider that groups CSV rows by TournamentId ---
  static Stream<Arguments> tournamentsFromCsv() throws Exception {
    URL url = TournamentBuilderTest.class.getResource("/io.github.redouanebali/tournament_scenarios.csv");
    if (url == null) {
      throw new IllegalStateException("tournament_scenarios.csv not found in test resources");
    }
    List<String> lines = Files.readAllLines(Path.of(url.toURI()));
    if (lines.size() <= 1) {
      throw new IllegalStateException("CSV appears empty");
    }
    buildHeaderIndex(lines.get(0));

    Map<String, List<String[]>> byTid = new LinkedHashMap<>();
    for (int r = 1; r < lines.size(); r++) {
      String line = lines.get(r);
      if (line.isBlank()) {
        continue;
      }
      String[] f   = line.split(",");
      String   tid = f[headerIndexFor("TournamentId")].trim();
      byTid.computeIfAbsent(tid, k -> new ArrayList<>()).add(f);
    }
    return byTid.entrySet().stream().map(e -> Arguments.of(e.getKey(), e.getValue()));
  }

  private static void buildHeaderIndex(String headerLine) {
    String[]             cols = headerLine.split(",");
    Map<String, Integer> map  = new HashMap<>();
    for (int i = 0; i < cols.length; i++) {
      map.put(cols[i].trim(), i);
    }
    headerIndex = map;
  }

  private static int headerIndexFor(String key) {
    if (headerIndex == null) {
      throw new IllegalStateException("Header index not initialized");
    }
    Integer idx = headerIndex.get(key);
    if (idx == null) {
      throw new IllegalArgumentException("Unknown CSV column: " + key);
    }
    return idx;
  }


  @ParameterizedTest(name = "Main draw only: mainDraw={0}")
  @CsvSource({
      "32, 8, SEEDED, R32;R16;QUARTERS;SEMIS;FINAL, 16;8;4;2;1",
      "64, 16, SEEDED, R64;R32;R16;QUARTERS;SEMIS;FINAL, 32;16;8;4;2;1"
  })
  void testBuild_mainDrawOnly_createsExpectedRoundsAndMatches(int mainDraw,
                                                              int nbSeedsMain,
                                                              DrawMode drawMode,
                                                              String expectedStagesCsv,
                                                              String expectedMatchesCsv) {
    Tournament tournament = TestFixtures.makeTournament(0, 0, mainDraw, nbSeedsMain, 0, drawMode);
    TournamentBuilder.setupAndPopulateTournament(tournament, TestFixtures.createPlayerPairs(mainDraw));
    List<Stage>   expectedStages  = TestFixtures.parseStages(expectedStagesCsv);
    List<Integer> expectedMatches = TestFixtures.parseInts(expectedMatchesCsv);
    List<Stage> actualStages = tournament.getRounds().stream()
                                         .map(Round::getStage)
                                         .toList();
    Assertions.assertEquals(expectedStages, actualStages);
    List<Integer> actualMatches = tournament.getRounds().stream()
                                            .map(r -> r.getGames().size())
                                            .toList();
    Assertions.assertEquals(expectedMatches, actualMatches);
  }

  @ParameterizedTest(name = "With qualifications: preQual={0} -> nbQualifiers={1}, mainDraw={2}")
  @CsvSource({
      // preQual, nbQualifiers, mainDraw, nbSeedsMain, nbSeedsQual, drawMode, expectedStages, expectedMatches
      "16, 4, 32, 8, 4, SEEDED, Q1;Q2;R32;R16;QUARTERS;SEMIS;FINAL, 8;4;16;8;4;2;1",
      "32, 8, 32, 8, 8, SEEDED, Q1;Q2;R32;R16;QUARTERS;SEMIS;FINAL, 16;8;16;8;4;2;1",
      "32, 8, 64, 16, 8, SEEDED, Q1;Q2;R64;R32;R16;QUARTERS;SEMIS;FINAL, 16;8;32;16;8;4;2;1"
  })
  void testBuild_withQualifications_createsExpectedRoundsAndMatches(int preQual,
                                                                    int nbQualifiers,
                                                                    int mainDraw,
                                                                    int nbSeedsMain,
                                                                    int nbSeedsQual,
                                                                    DrawMode drawMode,
                                                                    String expectedStagesCsv,
                                                                    String expectedMatchesCsv) {
    Tournament tournament = TestFixtures.makeTournament(preQual, nbQualifiers, mainDraw, nbSeedsMain, nbSeedsQual, drawMode);
    // Use public API: create empty tournament by providing empty player list
    TournamentBuilder.setupAndPopulateTournament(tournament, new ArrayList<>());
    List<Stage>   expectedStages  = parseStages(expectedStagesCsv);
    List<Integer> expectedMatches = parseInts(expectedMatchesCsv);
    List<Stage> actualStages = tournament.getRounds().stream()
                                         .map(Round::getStage)
                                         .toList();

    List<Integer> actualMatches = tournament.getRounds().stream()
                                            .map(r -> r.getGames() == null ? 0 : r.getGames().size())
                                            .toList();

    assertEquals(expectedStages, actualStages, "Stages sequence must match");
    assertEquals(expectedMatches, actualMatches, "Matches per stage must match");
    assertEquals(expectedStages.size(), tournament.getRounds().size(), "Unexpected number of rounds created");
  }

  // ============= UPDATED TESTS FOR DRAW STRATEGIES =============

  @Test
  void testAutomaticDrawStrategy_mainDrawOnly_fillsOnlyFirstRound() {
    // Given: Tournament with main draw only (32 players, 8 seeds)
    Tournament tournament = TestFixtures.makeTournament(0, 0, 32, 8, 0, DrawMode.SEEDED);

    // Create 20 player pairs (less than draw size to test BYE placement)
    List<PlayerPair> playerPairs = TestFixtures.createPlayerPairs(20);

    // When: Use the new setupTournamentWithPlayers method (replaces 5 manual steps)
    TournamentBuilder.setupAndPopulateTournament(tournament, playerPairs);

    // Then: Only the first round (R32) should be filled
    Round r32Round = tournament.getRoundByStage(Stage.R32);
    Round r16Round = tournament.getRoundByStage(Stage.R16);

    // R32 should be filled with teams and BYEs
    long teamsInR32 = r32Round.getGames().stream()
                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                              .filter(Objects::nonNull)
                              .count();
    assertEquals(32, teamsInR32, "R32 should have all slots filled");

    // R16 should be empty (no teams placed yet)
    long teamsInR16 = r16Round.getGames().stream()
                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                              .filter(Objects::nonNull)
                              .count();
    assertEquals(0, teamsInR16, "R16 should be empty before propagation");

    // Verify seeds are placed in R32
    long seedsInR32 = r32Round.getGames().stream()
                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                              .filter(Objects::nonNull)
                              .filter(p -> !p.isBye() && p.getSeed() > 0 && p.getSeed() <= 8)
                              .count();
    assertEquals(8, seedsInR32, "All 8 seeds should be placed in R32");

    // Additional checks for BYE placement and seed distribution
    long byesInR32 = r32Round.getGames().stream()
                             .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                             .filter(Objects::nonNull)
                             .filter(PlayerPair::isBye)
                             .count();
    assertEquals(12, byesInR32, "R32 should have 12 BYEs (32 - 20 teams)");

    // Verify no duplicate teams
    List<String> teamSignatures = r32Round.getGames().stream()
                                          .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                          .filter(Objects::nonNull)
                                          .filter(p -> !p.isBye())
                                          .map(p -> p.getPlayer1().getName() + "/" + p.getPlayer2().getName())
                                          .toList();
    long uniqueTeams = teamSignatures.stream().distinct().count();
    assertEquals(teamSignatures.size(), uniqueTeams, "No duplicate teams should exist in R32");

    // Verify seed positioning - seeds should be placed in specific theoretical positions
    List<Integer> seedPositions = new ArrayList<>();
    for (int i = 0; i < r32Round.getGames().size(); i++) {
      Game g = r32Round.getGames().get(i);
      if (g.getTeamA() != null && !g.getTeamA().isBye() && g.getTeamA().getSeed() > 0 && g.getTeamA().getSeed() <= 8) {
        seedPositions.add(i * 2); // Team A position
      }
      if (g.getTeamB() != null && !g.getTeamB().isBye() && g.getTeamB().getSeed() > 0 && g.getTeamB().getSeed() <= 8) {
        seedPositions.add(i * 2 + 1); // Team B position
      }
    }
    assertEquals(8, seedPositions.size(), "Should have exactly 8 seeded positions");
  }

  @Test
  void testAutomaticDrawStrategy_withQualifications_fillsQ1AndR32() {
    // Given: Tournament with qualifications (16 -> 4 qualifiers) + main draw (32 players, 8 seeds)
    Tournament tournament = TestFixtures.makeTournament(16, 4, 32, 8, 4, DrawMode.SEEDED);
    TournamentBuilder.setupAndPopulateTournament(tournament, new ArrayList<>());

    // Create 28 player pairs (16 for qualifs + 12 direct entry to main draw)
    List<PlayerPair> playerPairs = TestFixtures.createPlayerPairs(28);

    // When: Use the new strategy to fill initial rounds
    DrawStrategy drawStrategy = new AutomaticDrawStrategy();
    drawStrategy.placePlayers(tournament, playerPairs);

    // Then: Q1 and R32 should be filled, Q2 and R16 should be empty
    Round q1Round  = tournament.getRoundByStage(Stage.Q1);
    Round q2Round  = tournament.getRoundByStage(Stage.Q2);
    Round r32Round = tournament.getRoundByStage(Stage.R32);
    Round r16Round = tournament.getRoundByStage(Stage.R16);

    // Q1 should be filled
    long teamsInQ1 = q1Round.getGames().stream()
                            .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                            .filter(Objects::nonNull)
                            .count();
    assertEquals(16, teamsInQ1, "Q1 should have all slots filled");

    // Q2 should be empty
    long teamsInQ2 = q2Round.getGames().stream()
                            .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                            .filter(Objects::nonNull)
                            .count();
    assertEquals(0, teamsInQ2, "Q2 should be empty before propagation");

    // R32 should be filled with direct entries, qualifiers placeholders, and BYEs
    long teamsInR32 = r32Round.getGames().stream()
                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                              .filter(Objects::nonNull)
                              .count();
    assertEquals(32, teamsInR32, "R32 should have all slots filled");

    // R16 should be empty
    long teamsInR16 = r16Round.getGames().stream()
                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                              .filter(Objects::nonNull)
                              .count();
    assertEquals(0, teamsInR16, "R16 should be empty before propagation");
  }

  @Test
  void testDrawStrategy_edgeCases() {
    // Test edge cases for draw strategies
    DrawStrategy automaticStrategy = DrawStrategyFactory.createStrategy(DrawMode.SEEDED);
    DrawStrategy manualStrategy    = DrawStrategyFactory.createStrategy(DrawMode.MANUAL);

    // Case 1: Null tournament
    automaticStrategy.placePlayers(null, TestFixtures.createPlayerPairs(10));
    manualStrategy.placePlayers(null, TestFixtures.createPlayerPairs(10));
    // Should not throw exception
    assertTrue(true, "Should handle null tournament without throwing exception");

    // Case 2: Null player pairs
    Tournament tournament = TestFixtures.makeTournament(0, 0, 32, 8, 0, DrawMode.SEEDED);
    automaticStrategy.placePlayers(tournament, null);
    manualStrategy.placePlayers(tournament, null);
    // Should not throw exception

    // Case 3: Empty player pairs
    automaticStrategy.placePlayers(tournament, new ArrayList<>());
    manualStrategy.placePlayers(tournament, new ArrayList<>());
    // Should not throw exception

    // Case 4: Tournament with no rounds
    Tournament emptyTournament = TestFixtures.makeTournament(0, 0, 32, 8, 0, DrawMode.SEEDED);
    automaticStrategy.placePlayers(emptyTournament, TestFixtures.createPlayerPairs(10));
    manualStrategy.placePlayers(emptyTournament, TestFixtures.createPlayerPairs(10));
    // Should not throw exception
  }

  @Test
  void testDrawStrategy_onlyInitialRoundsAreFilled() {
    // Given: Tournament with qualifications and main draw
    Tournament tournament = TestFixtures.makeTournament(32, 8, 64, 16, 8, DrawMode.SEEDED);
    TournamentBuilder.setupAndPopulateTournament(tournament, new ArrayList<>());

    List<PlayerPair> playerPairs = TestFixtures.createPlayerPairs(48);

    // When: Use strategy to fill initial rounds
    DrawStrategy drawStrategy = new AutomaticDrawStrategy();
    drawStrategy.placePlayers(tournament, playerPairs);

    // Then: Only Q1 and R64 should be filled
    List<Stage> initialStages    = List.of(Stage.Q1, Stage.R64);
    List<Stage> subsequentStages = List.of(Stage.Q2, Stage.R32, Stage.R16, Stage.QUARTERS, Stage.SEMIS, Stage.FINAL);

    for (Stage stage : initialStages) {
      Round round = tournament.getRoundByStage(stage);
      long teamsCount = round.getGames().stream()
                             .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                             .filter(Objects::nonNull)
                             .count();
      assertTrue(teamsCount > 0, "Initial stage " + stage + " should have teams");
    }

    for (Stage stage : subsequentStages) {
      Round round = tournament.getRoundByStage(stage);
      long teamsCount = round.getGames().stream()
                             .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                             .filter(Objects::nonNull)
                             .count();
      assertEquals(0, teamsCount, "Subsequent stage " + stage + " should be empty before propagation");
    }
  }

  @Test
  void testAutomaticDrawStrategy_groupsMode_placesPairsInPools() {
    // Parameters for a tournament with 2 pools of 4 pairs
    int nbPools           = 2;
    int nbPairsPerPool    = 4;
    int nbQualifiedByPool = 2;
    int mainDrawSize      = 4;
    int nbSeedsMain       = 0;
    int totalPairs        = nbPools * nbPairsPerPool;

    TournamentConfig config = TournamentConfig.builder()
                                              .format(io.github.redouanebali.model.format.TournamentFormat.GROUPS_KO)
                                              .mainDrawSize(mainDrawSize)
                                              .nbSeeds(nbSeedsMain)
                                              .nbPools(nbPools)
                                              .nbPairsPerPool(nbPairsPerPool)
                                              .nbQualifiedByPool(nbQualifiedByPool)
                                              .drawMode(DrawMode.SEEDED)
                                              .build();
    Tournament tournament = new Tournament();
    tournament.setConfig(config);

    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(totalPairs);
    TournamentBuilder.setupAndPopulateTournament(tournament, pairs);

    Round groupsRound = tournament.getRounds().stream()
                                  .filter(r -> r.getStage() == Stage.GROUPS)
                                  .findFirst()
                                  .orElseThrow();

    // Verify the number of pools
    assertEquals(nbPools, groupsRound.getPools().size(), "Number of pools must be correct");
    // Verify the number of pairs per pool
    for (int i = 0; i < nbPools; i++) {
      assertEquals(nbPairsPerPool, groupsRound.getPools().get(i).getPairs().size(), "Each pool must contain the correct number of pairs");
    }
    // Verify that all pairs are placed and none are duplicated
    List<PlayerPair> allPlaced = groupsRound.getPools().stream()
                                            .flatMap(pool -> pool.getPairs().stream())
                                            .toList();
    assertEquals(totalPairs, allPlaced.size(), "All pairs must be placed in pools");
    long uniquePairs = allPlaced.stream().distinct().count();
    assertEquals(totalPairs, uniquePairs, "No pair should be duplicated in pools");
  }

  @Test
  void testAutomaticDrawStrategy_groupsMode_seedsAreSnakeDistributed() {
    // 4 pools, 4 seeds
    int nbPools           = 4;
    int nbPairsPerPool    = 4;
    int nbQualifiedByPool = 2;
    int mainDrawSize      = 8;
    int nbSeedsMain       = 4;
    int totalPairs        = nbPools * nbPairsPerPool;

    TournamentConfig config = TournamentConfig.builder()
                                              .format(TournamentFormat.GROUPS_KO)
                                              .mainDrawSize(mainDrawSize)
                                              .nbSeeds(nbSeedsMain)
                                              .nbPools(nbPools)
                                              .nbPairsPerPool(nbPairsPerPool)
                                              .nbQualifiedByPool(nbQualifiedByPool)
                                              .drawMode(DrawMode.SEEDED)
                                              .build();

    Tournament tournament = new Tournament();
    tournament.setConfig(config);

    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(totalPairs);
    // Assign seeds 1 to 4 to the first 4 pairs
    for (int i = 0; i < nbSeedsMain; i++) {
      pairs.get(i).setSeed(i + 1);
    }
    TournamentBuilder.setupAndPopulateTournament(tournament, pairs);

    Round groupsRound = tournament.getRounds().stream()
                                  .filter(r -> r.getStage() == Stage.GROUPS)
                                  .findFirst()
                                  .orElseThrow();

    // Verify snake distribution of seeds in pools
    // TS1 in A, TS2 in D, TS3 in C, TS4 in B
    String[] expectedPools = {"Pool A", "Pool D", "Pool C", "Pool B"};
    int[]    expectedSeeds = {1, 2, 3, 4};
    for (int i = 0; i < nbSeedsMain; i++) {
      AtomicBoolean found      = new AtomicBoolean(false);
      final int     seedToFind = expectedSeeds[i];
      for (int p = 0; p < nbPools; p++) {
        Pool    pool    = groupsRound.getPools().get(p);
        boolean hasSeed = pool.getPairs().stream().anyMatch(pair -> pair.getSeed() == seedToFind);
        if (hasSeed) {
          assertEquals(expectedPools[i], pool.getName(), "Seed " + expectedSeeds[i] + " must be in " + expectedPools[i]);
          found.set(true);
          break;
        }
      }
      assertTrue(found.get(), "Seed " + expectedSeeds[i] + " must be placed in a pool");
    }
  }

  @ParameterizedTest(name = "Qualifs: preQual={0}, nbQual={1}, mainDraw={2}, stages={3}")
  @CsvSource({
      // preQualDrawSize, nbQualifiers, mainDrawSize, expectedStagesCsv
      "32, 16, 32, Q1;R32;R16;QUARTERS;SEMIS;FINAL",
      "32, 8, 32, Q1;Q2;R32;R16;QUARTERS;SEMIS;FINAL",
      "16, 8, 32, Q1;R32;R16;QUARTERS;SEMIS;FINAL",
      "16, 4, 32, Q1;Q2;R32;R16;QUARTERS;SEMIS;FINAL",
      "64, 32, 64, Q1;R64;R32;R16;QUARTERS;SEMIS;FINAL",
      "64, 16, 64, Q1;Q2;R64;R32;R16;QUARTERS;SEMIS;FINAL",
      "8, 4, 16, Q1;R16;QUARTERS;SEMIS;FINAL",
      "8, 2, 16, Q1;Q2;R16;QUARTERS;SEMIS;FINAL"
  })
  void testSetupTournament_withQualifications_stagesOnly(
      int preQualDrawSize, int nbQualifiers, int mainDrawSize, String expectedStagesCsv) {
    TournamentConfig config = TournamentConfig.builder()
                                              .preQualDrawSize(preQualDrawSize)
                                              .nbQualifiers(nbQualifiers)
                                              .nbSeedsQualify(8)
                                              .mainDrawSize(mainDrawSize)
                                              .nbSeeds(16)
                                              .drawMode(DrawMode.SEEDED)
                                              .format(TournamentFormat.QUALIF_KO)
                                              .build();
    Tournament tournament = new Tournament();
    tournament.setConfig(config);

    TournamentBuilder.initializeEmptyRounds(tournament);

    List<Stage> expectedStages = parseStages(expectedStagesCsv);
    List<Stage> actualStages   = tournament.getRounds().stream().map(Round::getStage).toList();
    assertEquals(expectedStages, actualStages, "Rounds sequence must be correct");
  }

  @ParameterizedTest(name = "Config sans qualifs: mainDraw={0}, stages={1}")
  @CsvSource({
      // mainDrawSize, expectedStagesCsv
      "32, R32;R16;QUARTERS;SEMIS;FINAL",
      "64, R64;R32;R16;QUARTERS;SEMIS;FINAL",
      "16, R16;QUARTERS;SEMIS;FINAL"
  })
  void testSetupTournament_noQualifications_stagesOnly(
      int mainDrawSize, String expectedStagesCsv) {
    TournamentConfig config = TournamentConfig.builder()
                                              .mainDrawSize(mainDrawSize)
                                              .nbSeeds(8)
                                              .drawMode(DrawMode.SEEDED)
                                              .format(TournamentFormat.KNOCKOUT)
                                              .build();
    Tournament tournament = new Tournament();
    tournament.setConfig(config);

    TournamentBuilder.initializeEmptyRounds(tournament);

    List<Stage> expectedStages = parseStages(expectedStagesCsv);
    List<Stage> actualStages   = tournament.getRounds().stream().map(Round::getStage).toList();
    assertEquals(expectedStages, actualStages, "Rounds sequence must be correct");
  }


  @ParameterizedTest(name = "QUALIF_KO: preQual={0}, nbQualifiers={1}, mainDraw={2}, nbSeeds={3}")
  @CsvSource({
      "8, 4, 32, 4",
      "16, 8, 32, 8",
      "32, 16, 64, 16",
      "16, 4, 32, 8",
      "32, 8, 64, 16"
  })
  void testMainDrawHasExactNumberOfQualifiersInQualifKoMode(int preQualDrawSize, int nbQualifiers, int mainDrawSize, int nbSeeds) {
    int expectedQualifiers = nbQualifiers;
    int expectedTeams      = mainDrawSize - nbQualifiers;

    TournamentConfig config = TournamentConfig.builder()
                                              .preQualDrawSize(preQualDrawSize)
                                              .nbQualifiers(nbQualifiers)
                                              .mainDrawSize(mainDrawSize)
                                              .nbSeeds(nbSeeds)
                                              .drawMode(DrawMode.SEEDED)
                                              .format(TournamentFormat.QUALIF_KO)
                                              .build();

    Tournament tournament = new Tournament();
    tournament.setConfig(config);
    TournamentBuilder.initializeEmptyRounds(tournament);

    List<PlayerPair> allTeams = TestFixtures.createPlayerPairs(expectedTeams + preQualDrawSize);
    TournamentBuilder.setupAndPopulateTournament(tournament, allTeams);

    Round mainDrawRound = tournament.getRounds().stream()
                                    .filter(r -> r.getStage().isMainDraw(mainDrawSize))
                                    .findFirst()
                                    .orElseThrow();

    long qualifierCount = mainDrawRound.getGames().stream()
                                       .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                       .filter(p -> p != null && p.isQualifier())
                                       .count();
    long realTeamsCount = mainDrawRound.getGames().stream()
                                       .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                       .filter(p -> p != null && !p.isQualifier() && !p.isBye())
                                       .count();

    assertEquals(expectedQualifiers, qualifierCount, "Main draw must contain exactly " + expectedQualifiers + " QUALIFIER slots.");
    assertEquals(expectedTeams, realTeamsCount, "Main draw must contain exactly " + expectedTeams + " direct entry teams.");
  }

  // ============= TEST TO DETECT QUALIF/MAIN DRAW DUPLICATION BUG =============

  @Test
  void testQualifKO_noTeamShouldBeInBothQualifAndMainDraw() {
    Tournament tournament = TestFixtures.makeTournament(
        16,           // preQualSize = 16 places in qualif
        4,            // nbQualifiers = 4 qualifiers advance to main draw
        32,           // mainDrawSize = 32 places in final phase
        8,            // nbSeedsMain = 8 seeds in main draw
        4,            // nbSeedsQual = 4 seeds in qualif
        DrawMode.SEEDED
    );

    // Create 28 player pairs with seeds 1-28
    List<PlayerPair> playerPairs = TestFixtures.createPlayerPairs(28);
    for (int i = 0; i < playerPairs.size(); i++) {
      playerPairs.get(i).setSeed(i + 1);
    }

    // When: Generate draw automatically
    TournamentBuilder.setupAndPopulateTournament(tournament, playerPairs);

    // Then: Collect all teams in Q1 and R32
    Round q1Round  = tournament.getRoundByStage(Stage.Q1);
    Round r32Round = tournament.getRoundByStage(Stage.R32);

    // Collect all NON-BYE and NON-QUALIFIER teams in Q1
    List<PlayerPair> teamsInQ1 = q1Round.getGames().stream()
                                        .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                        .filter(Objects::nonNull)
                                        .filter(p -> !p.isBye() && !p.isQualifier()) // Exclude BYE and QUALIFIER placeholders
                                        .toList();

    // Collect all NON-BYE and NON-QUALIFIER teams in R32
    List<PlayerPair> teamsInR32 = r32Round.getGames().stream()
                                          .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                          .filter(Objects::nonNull)
                                          .filter(p -> !p.isBye() && !p.isQualifier()) // Exclude BYE and QUALIFIER placeholders
                                          .toList();

    // Verify that no team is present in both rounds
    for (PlayerPair teamInQ1 : teamsInQ1) {
      boolean isDuplicateInR32 = teamsInR32.stream()
                                           .anyMatch(teamInR32 ->
                                                         teamInR32.getPlayer1().getName().equals(teamInQ1.getPlayer1().getName()) &&
                                                         teamInR32.getPlayer2().getName().equals(teamInQ1.getPlayer2().getName())
                                           );

      Assertions.assertFalse(isDuplicateInR32,
                             String.format("BUG DETECTED! Team %s/%s (seed %d) is present in both Q1 AND R32!",
                                           teamInQ1.getPlayer1().getName(),
                                           teamInQ1.getPlayer2().getName(),
                                           teamInQ1.getSeed()));
    }

    // Additional verification: weakest seeds should be in Q1
    // and strongest seeds should be in R32
    if (!teamsInQ1.isEmpty() && !teamsInR32.isEmpty()) {
      int maxSeedInQ1 = teamsInQ1.stream()
                                 .mapToInt(PlayerPair::getSeed)
                                 .max()
                                 .orElse(0);

      int minSeedInR32 = teamsInR32.stream()
                                   .mapToInt(PlayerPair::getSeed)
                                   .filter(seed -> seed > 0 && seed < Integer.MAX_VALUE) // Exclude special seeds
                                   .min()
                                   .orElse(Integer.MAX_VALUE);

      // Seeds in Q1 must be higher (weaker) than those in R32
      assertTrue(maxSeedInQ1 > minSeedInR32,
                 String.format("Seeds in qualif (%d max) must be weaker than those in main draw (%d min)",
                               maxSeedInQ1, minSeedInR32));
    }

    // Verify total number of unique teams
    List<PlayerPair> allRealTeams = new ArrayList<>();
    allRealTeams.addAll(teamsInQ1);
    allRealTeams.addAll(teamsInR32);

    long uniqueTeamsCount = allRealTeams.stream()
                                        .map(p -> p.getPlayer1().getName() + "/" + p.getPlayer2().getName())
                                        .distinct()
                                        .count();

    assertEquals(allRealTeams.size(), uniqueTeamsCount,
                 "All teams must be unique (no duplication between Q1 and R32)");
  }

  @ParameterizedTest(name = "QUALIF_KO: preQual={0}, qualifiers={1}, mainDraw={2}, teams={3}")
  @CsvSource({
      "16, 4, 32, 28",   // 16 in qualif, 12 direct in R32 (28 total)
      "32, 8, 32, 32",   // 32 in qualif, 0 direct in R32 (32 total)
      "32, 8, 64, 60",   // 32 in qualif, 28 direct in R64 (60 total)
      "16, 4, 16, 16"    // 16 in qualif, 0 direct in R16 (16 total)
  })
  void testQualifKO_variousConfigurations_noTeamDuplication(int preQual, int nbQualifiers, int mainDraw, int totalTeams) {
    // Given: Various QUALIF_KO configurations
    Tournament tournament = TestFixtures.makeTournament(
        preQual,
        nbQualifiers,
        mainDraw,
        Math.min(mainDraw / 4, 16), // nbSeedsMain
        Math.min(preQual / 4, 8),   // nbSeedsQual
        DrawMode.SEEDED
    );

    List<PlayerPair> playerPairs = TestFixtures.createPlayerPairs(totalTeams);
    for (int i = 0; i < playerPairs.size(); i++) {
      playerPairs.get(i).setSeed(i + 1);
    }

    // When: Generate draw
    TournamentBuilder.setupAndPopulateTournament(tournament, playerPairs);

    // Then: Collect all non-BYE, non-QUALIFIER teams from Q1 and first main round
    Round qualRound = tournament.getRounds().stream()
                                .filter(r -> r.getStage().isQualification())
                                .findFirst()
                                .orElse(null);

    Round mainRound = tournament.getRounds().stream()
                                .filter(r -> !r.getStage().isQualification())
                                .findFirst()
                                .orElse(null);

    if (qualRound != null && mainRound != null) {
      List<PlayerPair> teamsInQual = qualRound.getGames().stream()
                                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                              .filter(Objects::nonNull)
                                              .filter(p -> !p.isBye() && !p.isQualifier())
                                              .toList();

      List<PlayerPair> teamsInMain = mainRound.getGames().stream()
                                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                              .filter(Objects::nonNull)
                                              .filter(p -> !p.isBye() && !p.isQualifier())
                                              .toList();

      // Check no team is in both rounds
      for (PlayerPair teamInQual : teamsInQual) {
        boolean isDuplicate = teamsInMain.stream()
                                         .anyMatch(teamInMain ->
                                                       teamInMain.getPlayer1().getName().equals(teamInQual.getPlayer1().getName()) &&
                                                       teamInMain.getPlayer2().getName().equals(teamInQual.getPlayer2().getName())
                                         );

        Assertions.assertFalse(isDuplicate,
                               String.format("Team %s/%s appears in both qualification (%s) and main draw (%s)",
                                             teamInQual.getPlayer1().getName(),
                                             teamInQual.getPlayer2().getName(),
                                             qualRound.getStage(),
                                             mainRound.getStage()));
      }
    }
  }

  /**
   * BUG REPRODUCTION TEST: Main draw incorrectly includes all qualification teams instead of placeholders.
   *
   * Scenario: QUALIF_KO tournament with: - 16 teams in qualification for 4 qualifying spots - 32 teams in main draw (28 direct entries + 4
   * qualifiers) - Total: 48 teams
   *
   * Expected behavior: - Q1 round: 16 teams (all qualif teams) - R32 round: 32 slots with: - 20 direct entry teams (seeds + non-seeds) - 4 QUALIFIED
   * placeholders (for winners from qualif) - 8 BYE placeholders
   *
   * Current bug: All 16 qualif teams are incorrectly placed in the main draw R32.
   */
  @Test
  void testBugMainDrawIncludesAllQualifTeamsInsteadOfPlaceholders() {
    // Given: QUALIF_KO tournament with 16 qualif -> 4 places, 32 main draw
    int preQualDrawSize = 16;
    int nbQualifiers    = 4;
    int mainDrawSize    = 32;
    int nbSeedsMain     = 8;
    int nbSeedsQualify  = 4;

    Tournament tournament = TestFixtures.makeTournament(
        preQualDrawSize,
        nbQualifiers,
        mainDrawSize,
        nbSeedsMain,
        nbSeedsQualify,
        DrawMode.MANUAL
    );

    // Create 36 player pairs: 16 for qualif + 20 direct entries for main draw
    // Main draw calculation: 32 slots - 4 qualifiers - 8 BYEs = 20 direct entries
    // Total: 16 (qualif) + 20 (direct) = 36 teams
    int              totalTeams  = 36;
    List<PlayerPair> playerPairs = TestFixtures.createPlayerPairs(totalTeams);

    // When: Setup and populate tournament
    TournamentBuilder.setupAndPopulateTournament(tournament, playerPairs);

    System.out.println("=== SETUP COMPLETE - ANALYZING RESULTS ===");
    System.out.println("Total player pairs provided: " + totalTeams);
    System.out.println("Config - preQualDrawSize: " + preQualDrawSize);
    System.out.println("Config - nbQualifiers: " + nbQualifiers);
    System.out.println("Config - mainDrawSize: " + mainDrawSize);
    System.out.println("Expected: 16 in Q1, 20 direct + 4 QUALIFIED + 8 BYE in R32");

    // Then: Verify qualification round Q1
    Round q1Round = tournament.getRoundByStage(Stage.Q1);
    long teamsInQ1 = q1Round.getGames().stream()
                            .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                            .filter(Objects::nonNull)
                            .filter(p -> !p.isBye())
                            .count();
    System.out.println("Q1 - Real teams: " + teamsInQ1);
    assertEquals(16, teamsInQ1, "Q1 should have exactly 16 teams from qualification");

    // Then: Verify main draw R32
    Round r32Round = tournament.getRoundByStage(Stage.R32);

    // Count QUALIFIED placeholders (should be exactly nbQualifiers = 4)
    long qualifiedPlaceholdersInR32 = r32Round.getGames().stream()
                                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                              .filter(Objects::nonNull)
                                              .filter(PlayerPair::isQualifier)
                                              .count();
    System.out.println("R32 - QUALIFIED placeholders: " + qualifiedPlaceholdersInR32);

    // Count BYE placeholders (should be exactly 8 for a 32-team draw with 20 direct entries + 4 qualifiers)
    long byesInR32 = r32Round.getGames().stream()
                             .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                             .filter(Objects::nonNull)
                             .filter(PlayerPair::isBye)
                             .count();
    System.out.println("R32 - BYE placeholders: " + byesInR32);

    long realTeamsInR32 = r32Round.getGames().stream()
                                  .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                  .filter(Objects::nonNull)
                                  .filter(p -> !p.isBye() && !p.isQualifier())
                                  .count();
    System.out.println("R32 - Real teams (direct entries): " + realTeamsInR32);
    System.out.println("R32 - Total: " + (qualifiedPlaceholdersInR32 + byesInR32 + realTeamsInR32));

    assertEquals(nbQualifiers, qualifiedPlaceholdersInR32,
                 "R32 must have exactly " + nbQualifiers + " QUALIFIED placeholders, not all qualification teams");
    assertEquals(8, byesInR32, "R32 should have exactly 8 BYE placeholders");

    // Count real direct entry teams (non-BYE, non-QUALIFIED)
    long directEntryTeamsInR32 = r32Round.getGames().stream()
                                         .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                         .filter(Objects::nonNull)
                                         .filter(p -> !p.isBye() && !p.isQualifier())
                                         .count();
    int expectedDirectEntries = mainDrawSize - nbQualifiers - 8; // 32 - 4 - 8 = 20
    assertEquals(expectedDirectEntries, directEntryTeamsInR32,
                 "R32 should have exactly " + expectedDirectEntries + " direct entry teams");

    // Verify total slots in R32 = 32
    long totalSlotsInR32 = r32Round.getGames().stream()
                                   .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                   .filter(Objects::nonNull)
                                   .count();
    assertEquals(mainDrawSize, totalSlotsInR32, "R32 should have exactly " + mainDrawSize + " total slots");

    // CRITICAL: Verify that qualification teams do NOT appear in main draw R32
    // Get all team IDs from Q1
    List<Long> qualTeamIds = q1Round.getGames().stream()
                                    .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                    .filter(Objects::nonNull)
                                    .filter(p -> !p.isBye())
                                    .map(PlayerPair::getId)
                                    .toList();

    // Get all direct entry team IDs from R32 (excluding BYE and QUALIFIED placeholders)
    List<Long> r32DirectEntryIds = r32Round.getGames().stream()
                                           .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                           .filter(Objects::nonNull)
                                           .filter(p -> !p.isBye() && !p.isQualifier())
                                           .map(PlayerPair::getId)
                                           .toList();

    // Check for overlap (this is the BUG - qualification teams should NOT be in main draw)
    List<Long> overlap = qualTeamIds.stream()
                                    .filter(r32DirectEntryIds::contains)
                                    .toList();

    assertTrue(overlap.isEmpty(),
               "BUG DETECTED: " + overlap.size() + " qualification teams are incorrectly placed in main draw R32. " +
               "Qualification teams should only appear as QUALIFIED placeholders after they win, not as direct entries.");
  }

  /**
   * CRITICAL TEST: 32 teams in qualification for 8 qualifying spots.
   *
   * Scenario: QUALIF_KO tournament with:
   * - 32 teams in qualification for 8 qualifying spots
   * - 32 teams in main draw
   * - Total: 32 teams (all participate in qualification, none directly in main draw)
   *
   * Expected main draw composition for a 32-slot draw:
   * - 8 QUALIFIED placeholders (for winners from qualification)
   * - 8 BYE placeholders
   * - 16 direct entry teams (strongest teams go directly to main draw)
   *
   * IMPORTANT: With 32 teams total competing for a 32-slot main draw with 8 qualifiers:
   * - Teams placed in Q1: 32 (all teams compete in qualification)
   * - Main draw R32: 8 qualifiers + 8 BYEs + 16 direct = 32 slots
   * - If user wants 24 direct entries, main draw must be 40 slots (8+8+24=40)
   */
  @Test
  void testCriticalQualifKO_32TeamsFor8QualifyingSpots() {
    // Given: QUALIF_KO tournament with 32 qualif -> 8 places, 32 main draw
    int preQualDrawSize = 32;   // 32 places in qualification
    int nbQualifiers    = 8;    // 8 qualifiers advance to main draw
    int mainDrawSize    = 32;   // 32 places in main draw (standard power of 2)
    int nbSeedsMain     = 8;    // 8 seeds in main draw
    int nbSeedsQualify  = 8;    // 8 seeds in qualif

    Tournament tournament = TestFixtures.makeTournament(
        preQualDrawSize,
        nbQualifiers,
        mainDrawSize,
        nbSeedsMain,
        nbSeedsQualify,
        DrawMode.MANUAL
    );

    // Calculate correct total teams:
    // Main draw = 32 slots = nbQualifiers + nbByes + directEntries
    // directEntries = 32 - 8 - 8 = 16
    // Total teams = teams in Q1 + directEntries
    // For 32 teams to compete in qualification (filling preQualDrawSize=32):
    //   totalTeams = 32 (Q1) + 16 (direct) = 48 total
    
    int teamsInQualif = preQualDrawSize;  // 32 teams compete in qualification
    int directEntries = 16;               // 16 teams go directly to main draw
    int totalTeams = teamsInQualif + directEntries;  // 48 total

    List<PlayerPair> playerPairs = TestFixtures.createPlayerPairs(totalTeams);

    System.out.println("\n=== CRITICAL TEST: 32 QUALIF PARTICIPANTS FOR 8 QUALIFYING SPOTS ===");
    System.out.println("Configuration:");
    System.out.println("  - Qualification draw size: " + preQualDrawSize);
    System.out.println("  - Number of qualifiers: " + nbQualifiers);
    System.out.println("  - Main draw size: " + mainDrawSize);
    System.out.println("  - Total teams provided: " + totalTeams);
    System.out.println("\nExpected distribution:");
    System.out.println("  - Q1: " + teamsInQualif + " real teams (filling all " + preQualDrawSize + " slots)");
    System.out.println("  - R32: " + nbQualifiers + " QUALIFIED placeholders");
    System.out.println("  - R32: 8 BYE placeholders");
    System.out.println("  - R32: " + directEntries + " direct entry teams");
    System.out.println("  - R32 Total: " + (nbQualifiers + 8 + directEntries) + " slots");

    // When: Setup and populate tournament
    TournamentBuilder.setupAndPopulateTournament(tournament, playerPairs);

    System.out.println("\n=== ACTUAL RESULTS ===");

    // Then: Verify qualification round Q1
    Round q1Round = tournament.getRoundByStage(Stage.Q1);
    long teamsInQ1 = q1Round.getGames().stream()
                            .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                            .filter(Objects::nonNull)
                            .filter(p -> !p.isBye())
                            .count();
    System.out.println("Q1 - Real teams: " + teamsInQ1);

    // Then: Verify main draw R32
    Round r32Round = tournament.getRoundByStage(Stage.R32);

    // Count QUALIFIED placeholders
    long qualifiedPlaceholdersInR32 = r32Round.getGames().stream()
                                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                              .filter(Objects::nonNull)
                                              .filter(PlayerPair::isQualifier)
                                              .count();
    System.out.println("R32 - QUALIFIED placeholders: " + qualifiedPlaceholdersInR32);

    // Count BYE placeholders
    long byesInR32 = r32Round.getGames().stream()
                             .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                             .filter(Objects::nonNull)
                             .filter(PlayerPair::isBye)
                             .count();
    System.out.println("R32 - BYE placeholders: " + byesInR32);

    // Count real direct entry teams
    long realTeamsInR32 = r32Round.getGames().stream()
                                  .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                  .filter(Objects::nonNull)
                                  .filter(p -> !p.isBye() && !p.isQualifier())
                                  .count();
    System.out.println("R32 - Real teams (direct entries): " + realTeamsInR32);
    System.out.println("R32 - Total: " + (qualifiedPlaceholdersInR32 + byesInR32 + realTeamsInR32));

    // Verify counts match expectations
    assertEquals(nbQualifiers, qualifiedPlaceholdersInR32,
                 "R32 must have exactly " + nbQualifiers + " QUALIFIED placeholders");
    assertEquals(8, byesInR32, "R32 should have exactly 8 BYE placeholders");

    // Direct entries should be: mainDrawSize - nbQualifiers - 8 BYEs
    int expectedDirectEntries = mainDrawSize - nbQualifiers - 8; // 32 - 8 - 8 = 16
    assertEquals(expectedDirectEntries, realTeamsInR32,
                 "R32 should have exactly " + expectedDirectEntries + " direct entry teams");

    // Verify total slots in R32 = 32
    long totalSlotsInR32 = r32Round.getGames().stream()
                                   .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                   .filter(Objects::nonNull)
                                   .count();
    assertEquals(mainDrawSize, totalSlotsInR32, "R32 should have exactly " + mainDrawSize + " total slots");

    System.out.println("\n=== ARITHMETIC EXPLANATION ===");
    System.out.println("For a 32-slot main draw with 8 qualifiers:");
    System.out.println("  32 slots = 8 qualifiers + 8 BYEs + 16 direct entries");
    System.out.println("\nIf you want 24 direct entries instead of 16:");
    System.out.println("  24 + 8 qualifiers + 8 BYEs = 40 slots");
    System.out.println("  This would require a 40-slot main draw (non-standard, not power of 2)");
    System.out.println("\nNote: Standard draw sizes are powers of 2: 16, 32, 64, 128, etc.");
  }
}
