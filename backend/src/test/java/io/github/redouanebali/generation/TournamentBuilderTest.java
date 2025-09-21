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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;

public class TournamentBuilderTest {

  // --- Small header index helpers shared by provider and test ---
  private static Map<String, Integer> HEADER_INDEX;

  // --- Provider that groups CSV rows by TournamentId ---
  static Stream<Arguments> tournamentsFromCsv() throws Exception {
    URL url = TournamentBuilderTest.class.getResource("/tournament_scenarios.csv");
    if (url == null) {
      throw new IllegalStateException("tournament_scenarios.csv not found in test resources");
    }
    List<String> lines = Files.readAllLines(Path.of(url.toURI()));
    if (lines.size() <= 1) {
      throw new IllegalStateException("CSV appears empty");
    }
    buildHeaderIndex(lines.getFirst());

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
    HEADER_INDEX = map;
  }

  private static int headerIndexFor(String key) {
    if (HEADER_INDEX == null) {
      throw new IllegalStateException("Header index not initialized");
    }
    Integer idx = HEADER_INDEX.get(key);
    if (idx == null) {
      throw new IllegalArgumentException("Unknown CSV column: " + key);
    }
    return idx;
  }

  private static int intValue(String[] row, String key) {
    return Integer.parseInt(row[headerIndexFor(key)].trim());
  }

  private static String stringValue(String[] row, String key) {
    return row[headerIndexFor(key)].trim();
  }

  @ParameterizedTest(name = "Main draw only: mainDraw={0}")
  @CsvSource({
      // mainDraw, nbSeedsMain, drawMode, expectedStages, expectedMatches
      "32, 8, SEEDED, R32;R16;QUARTERS;SEMIS;FINAL, 16;8;4;2;1",
      "64, 16, SEEDED, R64;R32;R16;QUARTERS;SEMIS;FINAL, 32;16;8;4;2;1"
  })
  void testBuild_mainDrawOnly_createsExpectedRoundsAndMatches(int mainDraw,
                                                              int nbSeedsMain,
                                                              DrawMode drawMode,
                                                              String expectedStagesCsv,
                                                              String expectedMatchesCsv) {
    Tournament        tournament = TestFixtures.makeTournament(0, 0, mainDraw, nbSeedsMain, 0, drawMode);
    TournamentBuilder builder    = new TournamentBuilder();
    // Utilisation de TestFixtures.createPairs pour générer les joueurs
    builder.setupAndPopulateTournament(tournament, TestFixtures.createPlayerPairs(mainDraw));
    List<Stage>   expectedStages  = TestFixtures.parseStages(expectedStagesCsv);
    List<Integer> expectedMatches = TestFixtures.parseInts(expectedMatchesCsv);
    List<Stage> actualStages = tournament.getRounds().stream()
                                         .map(Round::getStage)
                                         .collect(Collectors.toList());
    Assertions.assertEquals(expectedStages, actualStages);
    List<Integer> actualMatches = tournament.getRounds().stream()
                                            .map(r -> r.getGames().size())
                                            .collect(Collectors.toList());
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

    TournamentBuilder builder = new TournamentBuilder();
    // Use public API: create empty tournament by providing empty player list
    builder.setupAndPopulateTournament(tournament, new ArrayList<>());
    List<Stage>   expectedStages  = parseStages(expectedStagesCsv);
    List<Integer> expectedMatches = parseInts(expectedMatchesCsv);

    List<Stage> actualStages = tournament.getRounds().stream()
                                         .map(Round::getStage)
                                         .collect(Collectors.toList());

    List<Integer> actualMatches = tournament.getRounds().stream()
                                            .map(r -> r.getGames() == null ? 0 : r.getGames().size())
                                            .collect(Collectors.toList());

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
    TournamentBuilder builder = new TournamentBuilder();
    builder.setupAndPopulateTournament(tournament, playerPairs);

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
    Tournament        tournament = TestFixtures.makeTournament(16, 4, 32, 8, 4, DrawMode.SEEDED);
    TournamentBuilder builder    = new TournamentBuilder();
    // Use public API: create empty tournament structure by providing empty player list
    builder.setupAndPopulateTournament(tournament, new ArrayList<>());

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
    Tournament        tournament = TestFixtures.makeTournament(32, 8, 64, 16, 8, DrawMode.SEEDED);
    TournamentBuilder builder    = new TournamentBuilder();
    // Use public API: create empty tournament structure by providing empty player list
    builder.setupAndPopulateTournament(tournament, new ArrayList<>());

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
    // Paramètres pour un tournoi à 2 poules de 4 paires
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

    TournamentBuilder builder = new TournamentBuilder();
    List<PlayerPair>  pairs   = TestFixtures.createPlayerPairs(totalPairs);
    builder.setupAndPopulateTournament(tournament, pairs);

    Round groupsRound = tournament.getRounds().stream()
                                  .filter(r -> r.getStage() == Stage.GROUPS)
                                  .findFirst()
                                  .orElseThrow();

    // Vérifie le nombre de pools
    assertEquals(nbPools, groupsRound.getPools().size(), "Le nombre de pools doit être correct");
    // Vérifie le nombre de paires par pool
    for (int i = 0; i < nbPools; i++) {
      assertEquals(nbPairsPerPool, groupsRound.getPools().get(i).getPairs().size(), "Chaque pool doit contenir le bon nombre de paires");
    }
    // Vérifie que toutes les paires sont placées et aucune n'est dupliquée
    List<PlayerPair> allPlaced = groupsRound.getPools().stream()
                                            .flatMap(pool -> pool.getPairs().stream())
                                            .toList();
    assertEquals(totalPairs, allPlaced.size(), "Toutes les paires doivent être placées dans les pools");
    long uniquePairs = allPlaced.stream().distinct().count();
    assertEquals(totalPairs, uniquePairs, "Aucune paire ne doit être dupliquée dans les pools");
  }

  @Test
  void testAutomaticDrawStrategy_groupsMode_seedsAreSnakeDistributed() {
    // 4 poules, 4 seeds
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

    TournamentBuilder builder = new TournamentBuilder();
    List<PlayerPair>  pairs   = TestFixtures.createPlayerPairs(totalPairs);
    // Affecte les seeds 1 à 4 aux 4 premières paires
    for (int i = 0; i < nbSeedsMain; i++) {
      pairs.get(i).setSeed(i + 1);
    }
    builder.setupAndPopulateTournament(tournament, pairs);

    Round groupsRound = tournament.getRounds().stream()
                                  .filter(r -> r.getStage() == Stage.GROUPS)
                                  .findFirst()
                                  .orElseThrow();

    // Vérifie la répartition snake des seeds dans les pools
    // TS1 en A, TS2 en D, TS3 en C, TS4 en B
    String[] expectedPools = {"Pool A", "Pool D", "Pool C", "Pool B"};
    int[]    expectedSeeds = {1, 2, 3, 4};
    for (int i = 0; i < nbSeedsMain; i++) {
      AtomicBoolean found      = new AtomicBoolean(false);
      final int     seedToFind = expectedSeeds[i];
      for (int p = 0; p < nbPools; p++) {
        Pool    pool    = groupsRound.getPools().get(p);
        boolean hasSeed = pool.getPairs().stream().anyMatch(pair -> pair.getSeed() == seedToFind);
        if (hasSeed) {
          assertEquals(expectedPools[i], pool.getName(), "La seed " + expectedSeeds[i] + " doit être dans " + expectedPools[i]);
          found.set(true);
          break;
        }
      }
      assertTrue(found.get(), "La seed " + expectedSeeds[i] + " doit être placée dans une pool");
    }
  }
}
