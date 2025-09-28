package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.util.TestFixtures;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TournamentBuilderCsvTest {

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

  // --- Provider for group KO CSV ---
  static Stream<Arguments> tournamentsFromGroupsCsv() throws Exception {
    URL url = TournamentBuilderTest.class.getResource("/tournament_scenarios_groups.csv");
    if (url == null) {
      throw new IllegalStateException("tournament_scenarios_groups.csv not found in test resources");
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

  /**
   * End-to-end CSV-driven test (does not modify existing tests). For each TournamentId in src/test/resources/tournament_scenarios.csv, this test
   * builds the full structure with TournamentBuilder, then for each round row it populates matches according to CSV columns: - DefaultQualif -> Team
   * vs BYE (auto-qualification) - Matches      -> TeamA vs TeamB with a decided winner (TeamA) via a score - BYE          -> remaining BYEs paired as
   * BYE vs BYE (no winner produced) It verifies per round: - Stage order equals CSV order - Total pairs == FromPreviousRound + NewTeams + BYE +
   * DefaultQualif - PairsNonBye matches the number of non-BYE teams placed - PairsPlaying matches the number of A-vs-B (non-BYE) games - Propagation
   * moves exactly (Matches + DefaultQualif) teams to the next round (except FINAL)
   */
  @ParameterizedTest(name = "Full tournament CSV flow: {0}")
  @MethodSource("tournamentsFromCsv")
  void testBuildAndPropagate_FullTournament_FromCsv(Long tournamentId, List<String[]> rows) {
    // Build tournament config from the first row (first 6 CSV inputs)
    String[] first           = rows.getFirst();
    int      preQualDrawSize = intValue(first, "preQualDrawSize");
    int      nbQualifiers    = intValue(first, "nbQualifiers");
    int      mainDrawSize    = intValue(first, "mainDrawSize");
    int      nbSeedsMain     = intValue(first, "nbSeeds");
    int      nbPlayerPairs   = intValue(first, "nbPlayerPairs");

    Tournament tournament = new Tournament();
    tournament.setId(tournamentId);
    TournamentConfig cfg = TournamentConfig.builder()
                                           .preQualDrawSize(preQualDrawSize)
                                           .nbQualifiers(nbQualifiers)
                                           .mainDrawSize(mainDrawSize)
                                           .nbSeeds(nbSeedsMain)
                                           .nbSeedsQualify(0)
                                           .drawMode(DrawMode.SEEDED)
                                           .build();
    tournament.setConfig(cfg);

    // Use public API: create empty tournament structure by providing empty player list
    TournamentBuilder.setupAndPopulateTournament(tournament, new ArrayList<>());

    // Stage order must match CSV order
    List<Stage> expectedStages = rows.stream()
                                     .map(r -> stringValue(r, "Round").toUpperCase())
                                     .map(Stage::valueOf)
                                     .collect(Collectors.toList());
    List<Stage> actualStages = tournament.getRounds().stream().map(Round::getStage).collect(Collectors.toList());
    assertEquals(expectedStages, actualStages, "Stage order mismatch for " + tournamentId);

    Stage firstMainStage = expectedStages.stream()
                                         .filter(s -> !s.isQualification())
                                         .findFirst()
                                         .orElse(null);

    // --- Initialize the first main draw round (before the for loop) ---
    if (firstMainStage != null) {
      tournament.getRounds().stream()
                .filter(r -> r.getStage() == firstMainStage)
                .findFirst().ifPresent(thatRound -> initializeFirstMainDrawWithoutQualifiers(tournamentId, thatRound));
    }

    // Simulate each round row-by-row
    for (String[] row : rows) {
      Stage stage = Stage.valueOf(stringValue(row, "Round").toUpperCase());
      Round currentRound = tournament.getRounds().stream().filter(r -> r.getStage() == stage).findFirst()
                                     .orElseThrow(() -> new IllegalStateException("Round not found: " + stage + " for " + tournamentId));

      int expectedTotalPairs = intValue(row, "TotalPairs");
      int expectedNbGames    = intValue(row, "Matches");

      // Draw size sanity
      assertEquals(expectedTotalPairs / 2, currentRound.getGames().size(), "Unexpected number of games in " + stage);

      // --- Initialize rounds based on type ---
      if (stage == Stage.Q1) {
        // Q1: Always reset and initialize from scratch
        for (Game g : currentRound.getGames()) {
          g.setTeamA(null);
          g.setTeamB(null);
          g.setScore(null);
        }
        initializeQ1Round(tournamentId, currentRound);
      } else if (firstMainStage != null && stage == firstMainStage) {
        // Initialization for the first main round is now handled before the loop.
        // Only perform scoring logic here.
        scoreExistingMatches(currentRound, expectedNbGames);
      } else {
        // Score only the requested number of real matches (non-BYE vs non-BYE) for this round.
        scoreExistingMatches(currentRound, expectedNbGames);
      }

      if (stage == Stage.FINAL) {
        continue; // no next round to propagate into
      }

      // Vérifie le nombre total d'équipes réelles uniques présentes dans tout le tournoi
      // (non null, non BYE, et pas des placeholders QUALIFIER)
      long distinctRealTeams = tournament.getRounds().stream()
                                         .flatMap(r -> r.getGames().stream())
                                         .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                         .filter(Objects::nonNull)
                                         .filter(p -> !p.isBye())
                                         // exclure les slots de type QUALIFIER (placeholders avant propagation)
                                         .filter(p -> p.getType() == null || p.getType() != PairType.QUALIFIER)
                                         // créer une "signature" stable pour dédupliquer une paire (par noms joueurs si dispo, sinon par seed)
                                         .map(p -> {
                                           String p1   = p.getPlayer1().getName();
                                           String p2   = p.getPlayer2().getName();
                                           int    sd   = p.getSeed();
                                           String base = (p1 != null ? p1 : "?") + "/" + (p2 != null ? p2 : "?");
                                           return base + "#seed=" + sd;
                                         })
                                         .distinct()
                                         .count();

      assertEquals(nbPlayerPairs, (int) distinctRealTeams,
                   "Distinct real teams mismatch (non-BYE, non-QUALIFIER) for tournament " + tournamentId);
      // Score the requested number of real matches before propagation
      scoreExistingMatches(currentRound, expectedNbGames);

      // Propagate winners to the next round BEFORE validation
      TournamentBuilder.propagateWinners(tournament);

      // Validate propagation results
      validatePropagation(currentRound, row);

      // Validate current round composition
      validateRoundComposition(currentRound, row, stage, tournamentId);
    }
  }

  // New implementation: initializeQ1Round loads games from teams_t1.json for the given tournamentId
  private void initializeQ1Round(Long tournamentId, Round currentRound) {
    String resource = "";
    try {
      ObjectMapper mapper = new ObjectMapper();
      resource = String.format("/teams/teams_t%d.json", tournamentId);
      InputStream is = getClass().getResourceAsStream(resource);
      @SuppressWarnings("unchecked")
      Map<String, Object> entry = mapper.readValue(is, Map.class);
      if (entry != null && entry.get("firstQualifPhase") != null) {
        List<?>    gamesJson = (List<?>) entry.get("firstQualifPhase");
        List<Game> games     = new ArrayList<>();
        for (Object gameObj : gamesJson) {
          String json = mapper.writeValueAsString(gameObj);
          games.add(mapper.readValue(json, Game.class));
        }
        currentRound.getGames().clear();
        currentRound.getGames().addAll(games);
      }
    } catch (Exception ex) {
      throw new RuntimeException("Failed to initialize Q1 round from " + resource, ex);
    }
  }

  private void initializeFirstMainDrawWithoutQualifiers(Long tournamentId, Round currentRound) {
    try {
      ObjectMapper mapper   = new ObjectMapper();
      String       resource = String.format("/teams/teams_t%d.json", tournamentId);
      InputStream  is       = getClass().getResourceAsStream(resource);
      @SuppressWarnings("unchecked")
      Map<String, Object> entry = mapper.readValue(is, Map.class);
      if (entry != null && entry.get("firstMainPhase") != null) {
        List<?>    gamesJson = (List<?>) entry.get("firstMainPhase");
        List<Game> games     = new ArrayList<>();
        for (Object gameObj : gamesJson) {
          String json = mapper.writeValueAsString(gameObj);
          games.add(mapper.readValue(json, Game.class));
        }
        currentRound.getGames().clear();
        currentRound.getGames().addAll(games);

        // Validate and clean up any null teams in loaded games
        for (Game g : currentRound.getGames()) {
          if (g.getTeamA() == null) {
            g.setTeamA(PlayerPair.bye());
          }
          if (g.getTeamB() == null) {
            g.setTeamB(PlayerPair.bye());
          }
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException("Failed to initialize first main draw without qualifiers from teams_t1.json", ex);
    }
  }

  private void scoreExistingMatches(Round currentRound, int expectedNbGames) {
    int toScore = expectedNbGames;
    for (Game g : currentRound.getGames()) {
      if (toScore == 0) {
        break;
      }
      if (g.getTeamA() != null && g.getTeamB() != null &&
          !g.getTeamA().isBye() && !g.getTeamB().isBye() &&
          g.getScore() == null) {
        g.setFormat(TestFixtures.createSimpleFormat(1));
        g.setScore(TestFixtures.createScoreWithWinner(g, g.getTeamA()));
        toScore--;
      }
    }
  }

  private void validatePropagation(Round round, String[] row) {
    for (Game game : round.getGames()) {
      Assertions.assertNotNull(game.getTeamA(), "teamA should not be null in round " + round.getStage());
      Assertions.assertNotNull(game.getTeamB(), "teamB should not be null in round " + round.getStage());
    }

    int expectedTotalPairs   = intValue(row, "TotalPairs");
    int expectedNonByePairs  = intValue(row, "PairsNonBye");
    int expectedPairsPlaying = intValue(row, "PairsPlaying");
    int expectedNbGames      = intValue(row, "Matches");

    int actualTotalPairs = round.getGames().size() * 2;
    assertEquals(expectedTotalPairs, actualTotalPairs, "Mismatch in total pairs for " + round.getStage());

    long actualNonByePairs = round.getGames().stream()
                                  .flatMap(game -> Stream.of(game.getTeamA(), game.getTeamB()))
                                  .filter(team -> !team.isBye())
                                  .count();
    assertEquals(expectedNonByePairs, actualNonByePairs, "Mismatch in non-BYE pairs for " + round.getStage());

    long actualPairsPlaying = round.getGames().stream()
                                   .filter(game -> !game.getTeamA().isBye() && !game.getTeamB().isBye())
                                   .count() * 2;
    assertEquals(expectedPairsPlaying, actualPairsPlaying, "Mismatch in pairs playing for " + round.getStage());

    long actualGames = round.getGames().stream()
                            .filter(game -> !game.getTeamA().isBye() && !game.getTeamB().isBye())
                            .count();
    assertEquals(expectedNbGames, actualGames, "Mismatch in number of games for " + round.getStage());
  }

  private void validateRoundComposition(Round currentRound, String[] row, Stage stage, Long tournamentId) {
    int expectedNonByePairs              = intValue(row, "PairsNonBye");
    int expectedPairsPlaying             = intValue(row, "PairsPlaying");
    int expectedTotalPairs               = intValue(row, "TotalPairs");
    int expectedByePairs                 = intValue(row, "BYE");
    int expectedNbDirectlyQualifiedPairs = intValue(row, "DefaultQualif");
    int fromPrev                         = intValue(row, "FromPreviousRound");
    int newTeams                         = intValue(row, "NewTeams");

    // Validate PairsNonBye
    long computedPairsNonBye = currentRound.getGames().stream()
                                           .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                           .filter(Objects::nonNull)
                                           .filter(p -> !p.isBye())
                                           .count();
    assertEquals(expectedNonByePairs, (int) computedPairsNonBye,
                 "PairsNonBye mismatch in " + stage + " for " + tournamentId);

    // Validate PairsPlaying
    int computedPairsPlaying = currentRound.getGames().stream()
                                           .filter(g -> g.getTeamA() != null && g.getTeamB() != null)
                                           .filter(g -> !g.getTeamA().isBye() && !g.getTeamB().isBye())
                                           .mapToInt(g -> 2)
                                           .sum();
    assertEquals(expectedPairsPlaying, computedPairsPlaying,
                 "PairsPlaying mismatch in " + stage + " for " + tournamentId);

    // Vérification : chaque équipe réelle apparaît exactement une fois dans le round (pas de doublon, pas d’oubli)
    List<String> teamSignatures = currentRound.getGames().stream()
                                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                              .filter(Objects::nonNull)
                                              // On ignore les paires BYE/BYE (type BYE ou les deux joueurs nommés "BYE")
                                              .filter(p -> !(p.isBye() ||
                                                             (p.getPlayer1() != null && "BYE".equals(p.getPlayer1().getName()) &&
                                                              p.getPlayer2() != null && "BYE".equals(p.getPlayer2().getName()))))
                                              .map(p -> {
                                                String p1   = p.getPlayer1() != null ? p.getPlayer1().getName() : "?";
                                                String p2   = p.getPlayer2() != null ? p.getPlayer2().getName() : "?";
                                                int    seed = p.getSeed();
                                                return p1 + "/" + p2 + "#seed=" + seed;
                                              })
                                              .toList();

    long uniqueTeams = teamSignatures.stream().distinct().count();
    assertEquals(teamSignatures.size(), uniqueTeams,
                 "Doublon ou oubli d'équipe réelle dans le round " + stage + " du tournoi " + tournamentId);

    // Cross-check composition
    assertEquals(expectedTotalPairs, fromPrev + newTeams + expectedByePairs + expectedNbDirectlyQualifiedPairs,
                 "Total pairs composition mismatch in " + stage + " for " + tournamentId);
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


  // @todo still KO
  @ParameterizedTest(name = "Full tournament group KO CSV flow: {0}")
  @MethodSource("tournamentsFromGroupsCsv")
  @Disabled
  void testBuildAndPropagate_FullTournamentGroupKo_FromCsv(Long tournamentId, List<String[]> rows) {
    // Utilisation des colonnes du CSV : nbPlayerPairs, nbPools, nbPairsPerPool, nbQualifiedByPool, mainDrawSize, nbSeeds, Round, etc.
    String[] first             = rows.getFirst();
    int      nbPlayerPairs     = intValue(first, "nbPlayerPairs");
    int      nbPools           = intValue(first, "nbPools");
    int      nbPairsPerPool    = intValue(first, "nbPairsPerPool");
    int      nbQualifiedByPool = intValue(first, "nbQualifiedByPool");
    int      mainDrawSize      = intValue(first, "mainDrawSize");
    int      nbSeedsMain       = intValue(first, "nbSeeds");

    Tournament tournament = new Tournament();
    tournament.setId(tournamentId);
    TournamentConfig cfg = TournamentConfig.builder()
                                           .mainDrawSize(mainDrawSize)
                                           .nbSeeds(nbSeedsMain)
                                           .nbPools(nbPools)
                                           .nbPairsPerPool(nbPairsPerPool)
                                           .nbQualifiedByPool(nbQualifiedByPool)
                                           .drawMode(DrawMode.SEEDED)
                                           .format(TournamentFormat.GROUPS_KO)
                                           .build();
    tournament.setConfig(cfg);

    List<PlayerPair> playerPairs = TestFixtures.createPlayerPairs(nbPlayerPairs);
    TournamentBuilder.setupAndPopulateTournament(tournament, playerPairs);

    List<Stage> expectedStages = rows.stream()
                                     .map(r -> stringValue(r, "Round").toUpperCase())
                                     .map(Stage::valueOf)
                                     .collect(Collectors.toList());
    List<Stage> actualStages = tournament.getRounds().stream().map(Round::getStage).collect(Collectors.toList());
    assertEquals(expectedStages, actualStages, "Stage order mismatch for " + tournamentId);

    Stage firstMainStage = expectedStages.stream()
                                         .filter(s -> !s.isQualification())
                                         .findFirst()
                                         .orElse(null);

    if (firstMainStage != null && firstMainStage != Stage.GROUPS) {
      tournament.getRounds().stream()
                .filter(r -> r.getStage() == firstMainStage)
                .findFirst().ifPresent(thatRound -> initializeFirstMainDrawWithoutQualifiers(tournamentId, thatRound));
    }

    for (String[] row : rows) {
      Stage stage = Stage.valueOf(stringValue(row, "Round").toUpperCase());
      Round currentRound = tournament.getRounds().stream().filter(r -> r.getStage() == stage).findFirst()
                                     .orElseThrow(() -> new IllegalStateException("Round not found: " + stage + " for " + tournamentId));

      int expectedTotalPairs               = intValue(row, "TotalPairs");
      int expectedNbGames                  = intValue(row, "Matches");
      int expectedPairsNonBye              = intValue(row, "PairsNonBye");
      int expectedPairsPlaying             = intValue(row, "PairsPlaying");
      int expectedByePairs                 = intValue(row, "BYE");
      int expectedNbDirectlyQualifiedPairs = intValue(row, "DefaultQualif");
      int fromPrev                         = intValue(row, "FromPreviousRound");
      int newTeams                         = intValue(row, "NewTeams");

      assertEquals(expectedNbGames, currentRound.getGames().size(), "Unexpected number of games in " + stage);

      if (stage == Stage.GROUPS) {
        for (Game g : currentRound.getGames()) {
          g.setTeamA(null);
          g.setTeamB(null);
          g.setScore(null);
        }
        initializeGroupsRound(tournament, currentRound);
      } else if (firstMainStage != null && stage == firstMainStage) {
        scoreExistingMatches(currentRound, expectedNbGames);
      } else {
        scoreExistingMatches(currentRound, expectedNbGames);
      }

      if (stage == Stage.FINAL) {
        continue;
      }

      long distinctRealTeams = tournament.getRounds().stream()
                                         .flatMap(r -> r.getGames().stream())
                                         .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                         .filter(Objects::nonNull)
                                         .filter(p -> !p.isBye())
                                         .filter(p -> p.getType() == null || p.getType() != PairType.QUALIFIER)
                                         .map(p -> {
                                           String p1   = p.getPlayer1().getName();
                                           String p2   = p.getPlayer2().getName();
                                           int    sd   = p.getSeed();
                                           String base = (p1 != null ? p1 : "?") + "/" + (p2 != null ? p2 : "?");
                                           return base + "#seed=" + sd;
                                         })
                                         .distinct()
                                         .count();

      assertEquals(nbPlayerPairs, (int) distinctRealTeams,
                   "Distinct real teams mismatch (non-BYE, non-QUALIFIER) for tournament " + tournamentId);
      scoreExistingMatches(currentRound, expectedNbGames);
      TournamentBuilder.propagateWinners(tournament);

      // Validation adaptée aux colonnes du CSV
      int actualTotalPairs = currentRound.getGames().size() * 2;
      assertEquals(expectedTotalPairs, actualTotalPairs, "Mismatch in total pairs for " + stage);

      long actualNonByePairs = currentRound.getGames().stream()
                                           .flatMap(game -> Stream.of(game.getTeamA(), game.getTeamB()))
                                           .filter(team -> !team.isBye())
                                           .count();
      assertEquals(expectedPairsNonBye, actualNonByePairs, "Mismatch in non-BYE pairs for " + stage);

      long actualPairsPlaying = currentRound.getGames().stream()
                                            .filter(game -> !game.getTeamA().isBye() && !game.getTeamB().isBye())
                                            .count() * 2;
      assertEquals(expectedPairsPlaying, actualPairsPlaying, "Mismatch in pairs playing for " + stage);

      long actualGames = currentRound.getGames().stream()
                                     .filter(game -> !game.getTeamA().isBye() && !game.getTeamB().isBye())
                                     .count();
      assertEquals(expectedNbGames, actualGames, "Mismatch in number of games for " + stage);

      // Cross-check composition
      assertEquals(expectedTotalPairs, fromPrev + newTeams + expectedByePairs + expectedNbDirectlyQualifiedPairs,
                   "Total pairs composition mismatch in " + stage + " for " + tournamentId);
    }
  }

  private void initializeGroupsRound(Tournament tournament, Round currentRound) {
    List<Game> games         = new ArrayList<>();
    int        expectedGames = tournament.getConfig().getNbPools() * (tournament.getConfig().getNbPairsPerPool() - 1) / 2;

    for (int i = 0; i < expectedGames; i++) {
      games.add(new Game());
    }
    currentRound.getGames().clear();
    currentRound.getGames().addAll(games);
  }
}
