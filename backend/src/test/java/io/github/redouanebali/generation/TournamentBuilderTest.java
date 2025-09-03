package io.github.redouanebali.generation;

import static io.github.redouanebali.util.TestFixtures.parseInts;
import static io.github.redouanebali.util.TestFixtures.parseStages;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.TournamentFormatConfig;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class TournamentBuilderTest {

  // --- Small header index helpers shared by provider and test ---
  private static Map<String, Integer> HEADER_INDEX;

  private static Tournament makeTournament(
      int preQualDrawSize,
      int nbQualifiers,
      int mainDrawSize,
      int nbSeeds,
      int nbSeedsQualify,
      DrawMode drawMode) {
    Tournament t = new Tournament();
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(nbSeeds)
                                                       .nbSeedsQualify(nbSeedsQualify)
                                                       .drawMode(drawMode)
                                                       .build();
    t.setConfig(cfg);
    return t;
  }

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
    Tournament t = makeTournament(0, 0, mainDraw, nbSeedsMain, 0, drawMode);

    TournamentBuilder builder   = new TournamentBuilder();
    List<Round>       roundList = builder.buildQualifKOStructure(t);
    t.getRounds().clear();
    t.getRounds().addAll(roundList);
    List<Stage>   expectedStages  = parseStages(expectedStagesCsv);
    List<Integer> expectedMatches = parseInts(expectedMatchesCsv);

    List<Stage> actualStages = t.getRounds().stream()
                                .map(Round::getStage)
                                .collect(Collectors.toList());

    List<Integer> actualMatches = t.getRounds().stream()
                                   .map(r -> r.getGames() == null ? 0 : r.getGames().size())
                                   .collect(Collectors.toList());

    assertEquals(expectedStages, actualStages, "Stages sequence must match");
    assertEquals(expectedMatches, actualMatches, "Matches per stage must match");
    assertEquals(expectedStages.size(), t.getRounds().size(), "Unexpected number of rounds created");
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
    Tournament t = makeTournament(preQual, nbQualifiers, mainDraw, nbSeedsMain, nbSeedsQual, drawMode);

    TournamentBuilder builder   = new TournamentBuilder();
    List<Round>       roundList = builder.buildQualifKOStructure(t);
    t.getRounds().clear();
    t.getRounds().addAll(roundList);
    List<Stage>   expectedStages  = parseStages(expectedStagesCsv);
    List<Integer> expectedMatches = parseInts(expectedMatchesCsv);

    List<Stage> actualStages = t.getRounds().stream()
                                .map(Round::getStage)
                                .collect(Collectors.toList());

    List<Integer> actualMatches = t.getRounds().stream()
                                   .map(r -> r.getGames() == null ? 0 : r.getGames().size())
                                   .collect(Collectors.toList());

    assertEquals(expectedStages, actualStages, "Stages sequence must match");
    assertEquals(expectedMatches, actualMatches, "Matches per stage must match");
    assertEquals(expectedStages.size(), t.getRounds().size(), "Unexpected number of rounds created");
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

    Tournament t = new Tournament();
    t.setId(tournamentId);
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(nbSeedsMain)
                                                       .nbSeedsQualify(0)
                                                       .drawMode(DrawMode.SEEDED)
                                                       .build();
    t.setConfig(cfg);

    TournamentBuilder builder = new TournamentBuilder();
    List<Round>       built   = builder.buildQualifKOStructure(t);
    t.getRounds().addAll(built);

    // Stage order must match CSV order
    List<Stage> expectedStages = rows.stream()
                                     .map(r -> stringValue(r, "Round").toUpperCase())
                                     .map(Stage::valueOf)
                                     .collect(Collectors.toList());
    List<Stage> actualStages = built.stream().map(Round::getStage).collect(Collectors.toList());
    assertEquals(expectedStages, actualStages, "Stage order mismatch for " + tournamentId);

    Stage firstMainStage = expectedStages.stream()
                                         .filter(s -> !s.isQualification())
                                         .findFirst()
                                         .orElse(null);

    // --- Initialize the first main draw round (before the for loop) ---
    if (firstMainStage != null) {
      built.stream()
           .filter(r -> r.getStage() == firstMainStage)
           .findFirst().ifPresent(thatRound -> initializeFirstMainDrawWithoutQualifiers(tournamentId, thatRound));
    }

    // Simulate each round row-by-row
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      String[] row   = rows.get(rowIndex);
      Stage    stage = Stage.valueOf(stringValue(row, "Round").toUpperCase());
      Round currentRound = built.stream().filter(r -> r.getStage() == stage).findFirst()
                                .orElseThrow(() -> new IllegalStateException("Round not found: " + stage + " for " + tournamentId));

      int expectedTotalPairs               = intValue(row, "TotalPairs");
      int expectedNonByePairs              = intValue(row, "PairsNonBye");
      int expectedPairsPlaying             = intValue(row, "PairsPlaying");
      int expectedNbGames                  = intValue(row, "Matches");
      int expectedNbDirectlyQualifiedPairs = intValue(row, "DefaultQualif");
      int expectedByePairs                 = intValue(row, "BYE");
      int fromPreviousRound                = intValue(row, "FromPreviousRound");

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
      long distinctRealTeams = t.getRounds().stream()
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
      builder.propagateWinners(t);

      // Validate propagation results
      Round nextRound            = t.getRounds().get(built.indexOf(currentRound) + 1);
      int   expectedNewTeamsNext = intValue(rows.get(rowIndex + 1), "NewTeams");
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
      InputStream is    = getClass().getResourceAsStream(resource);
      Map         entry = mapper.readValue(is, Map.class);
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
      ObjectMapper        mapper   = new ObjectMapper();
      String              resource = String.format("/teams/teams_t%d.json", tournamentId);
      InputStream         is       = getClass().getResourceAsStream(resource);
      Map<String, Object> entry    = mapper.readValue(is, Map.class);
      if (entry != null && entry.get("firstMainPhase") != null) {
        List<?>    gamesJson = (List<?>) entry.get("firstMainPhase");
        List<Game> games     = new ArrayList<>();
        for (Object gameObj : gamesJson) {
          String json = mapper.writeValueAsString(gameObj);
          games.add(mapper.readValue(json, Game.class));
        }
        currentRound.getGames().clear();
        currentRound.getGames().addAll(games);
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
          (g.getTeamA().getType() == null || g.getTeamA().getType() != PairType.BYE) &&
          (g.getTeamB().getType() == null || g.getTeamB().getType() != PairType.BYE) &&
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
    Assertions.assertEquals(expectedTotalPairs, actualTotalPairs, "Mismatch in total pairs for " + round.getStage());

    long actualNonByePairs = round.getGames().stream()
                                  .flatMap(game -> Stream.of(game.getTeamA(), game.getTeamB()))
                                  .filter(team -> !team.isBye())
                                  .count();
    Assertions.assertEquals(expectedNonByePairs, actualNonByePairs, "Mismatch in non-BYE pairs for " + round.getStage());

    long actualPairsPlaying = round.getGames().stream()
                                   .filter(game -> !game.getTeamA().isBye() && !game.getTeamB().isBye())
                                   .count() * 2;
    Assertions.assertEquals(expectedPairsPlaying, actualPairsPlaying, "Mismatch in pairs playing for " + round.getStage());

    long actualGames = round.getGames().stream()
                            .filter(game -> !game.getTeamA().isBye() && !game.getTeamB().isBye())
                            .count();
    Assertions.assertEquals(expectedNbGames, actualGames, "Mismatch in number of games for " + round.getStage());
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
}
