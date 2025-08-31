package io.github.redouanebali.generationV2;

import static io.github.redouanebali.util.TestFixtures.parseInts;
import static io.github.redouanebali.util.TestFixtures.parseStages;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.TournamentFormatConfig;
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

    TournamentBuilder builder = new TournamentBuilder();
    List<Round>       built   = builder.buildQualifKO(t);

    List<Stage>   expectedStages  = parseStages(expectedStagesCsv);
    List<Integer> expectedMatches = parseInts(expectedMatchesCsv);

    List<Stage> actualStages = built.stream()
                                    .map(Round::getStage)
                                    .collect(Collectors.toList());

    List<Integer> actualMatches = built.stream()
                                       .map(r -> r.getGames() == null ? 0 : r.getGames().size())
                                       .collect(Collectors.toList());

    assertEquals(expectedStages, actualStages, "Stages sequence must match");
    assertEquals(expectedMatches, actualMatches, "Matches per stage must match");
    assertEquals(expectedStages.size(), built.size(), "Unexpected number of rounds created");
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

    TournamentBuilder builder = new TournamentBuilder();
    List<Round>       built   = builder.buildQualifKO(t);

    List<Stage>   expectedStages  = parseStages(expectedStagesCsv);
    List<Integer> expectedMatches = parseInts(expectedMatchesCsv);

    List<Stage> actualStages = built.stream()
                                    .map(Round::getStage)
                                    .collect(Collectors.toList());

    List<Integer> actualMatches = built.stream()
                                       .map(r -> r.getGames() == null ? 0 : r.getGames().size())
                                       .collect(Collectors.toList());

    assertEquals(expectedStages, actualStages, "Stages sequence must match");
    assertEquals(expectedMatches, actualMatches, "Matches per stage must match");
    assertEquals(expectedStages.size(), built.size(), "Unexpected number of rounds created");
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
    String[] first           = rows.get(0);
    int      preQualDrawSize = intValue(first, "preQualDrawSize");
    int      nbQualifiers    = intValue(first, "nbQualifiers");
    int      mainDrawSize    = intValue(first, "mainDrawSize");
    int      nbSeedsMain     = intValue(first, "nbSeeds");

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
    List<Round>       built   = builder.buildQualifKO(t);
    t.getRounds().addAll(built);

    // Stage order must match CSV order
    List<Stage> expectedStages = rows.stream()
                                     .map(r -> stringValue(r, "Round").toUpperCase())
                                     .map(Stage::valueOf)
                                     .collect(Collectors.toList());
    List<Stage> actualStages = built.stream().map(Round::getStage).collect(Collectors.toList());
    assertEquals(expectedStages, actualStages, "Stage order mismatch for " + tournamentId);

    // Simulate each round row-by-row
    for (int i = 0; i < rows.size(); i++) {
      String[] row   = rows.get(i);
      Stage    stage = Stage.valueOf(stringValue(row, "Round").toUpperCase());
      Round currentRound = built.stream().filter(r -> r.getStage() == stage).findFirst()
                                .orElseThrow(() -> new IllegalStateException("Round not found: " + stage + " for " + tournamentId));

      int TotalPairs    = intValue(row, "TotalPairs");
      int pairsNonBye   = intValue(row, "PairsNonBye");
      int pairsPlaying  = intValue(row, "PairsPlaying");
      int matches       = intValue(row, "Matches");
      int defaultQualif = intValue(row, "DefaultQualif");
      int byeEntries    = intValue(row, "BYE");

      // Draw size sanity
      assertEquals(TotalPairs / 2, currentRound.getGames().size(), "Unexpected number of games in " + stage);

      // Reset the round content
      for (Game g : currentRound.getGames()) {
        g.setTeamA(null);
        g.setTeamB(null);
        g.setScore(null);
      }

      // 1) DefaultQualif: Team vs BYE (auto-qualification)
      int gi = 0;
      for (int d = 0; d < defaultQualif; d++) {
        Game g = currentRound.getGames().get(gi++);
        g.setTeamA(TestFixtures.buildPairWithSeed(1000 + d));
        g.setTeamB(PlayerPair.bye());
      }

      // 2) Matches: A vs B with a decided winner (TeamA)
      for (int m = 0; m < matches; m++) {
        Game       g  = currentRound.getGames().get(gi++);
        PlayerPair A  = TestFixtures.buildPairWithSeed(2000 + m * 2);
        PlayerPair Bp = TestFixtures.buildPairWithSeed(2000 + m * 2 + 1);
        g.setTeamA(A);
        g.setTeamB(Bp);
        g.setFormat(TestFixtures.createSimpleFormat(1));
        g.setScore(TestFixtures.createScoreWithWinner(g, A));
      }

      // 3) Remaining BYEs: pair as BYE vs BYE so they do not produce winners
      int byesLeft = Math.max(0, byeEntries - defaultQualif);
      while (byesLeft >= 2 && gi < currentRound.getGames().size()) {
        Game g = currentRound.getGames().get(gi++);
        g.setTeamA(PlayerPair.bye());
        g.setTeamB(PlayerPair.bye());
        byesLeft -= 2;
      }
      if (byesLeft == 1 && gi < currentRound.getGames().size()) {
        currentRound.getGames().get(gi).setTeamA(PlayerPair.bye());
      }

      // Validate PairsNonBye and PairsPlaying against CSV for this round
      long computedPairsNonBye = currentRound.getGames().stream()
                                             .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                             .filter(Objects::nonNull)
                                             .filter(p -> !p.isBye())
                                             .count();
      assertEquals(pairsNonBye, (int) computedPairsNonBye, "PairsNonBye mismatch in " + stage + " for " + tournamentId);

      // Count TEAMS that actually play (each real match contributes two non-BYE teams)
      int computedPairsPlaying = currentRound.getGames().stream()
                                             .filter(g -> g.getTeamA() != null && g.getTeamB() != null)
                                             .filter(g -> !g.getTeamA().isBye() && !g.getTeamB().isBye())
                                             .mapToInt(g -> 2)
                                             .sum();
      assertEquals(pairsPlaying, computedPairsPlaying,
                   "PairsPlaying mismatch in " + stage + " for " + tournamentId);

      if (stage == Stage.FINAL) {
        continue; // no next round to propagate into
      }

      // Pre-reserve QUALIFIER placeholders in the next round according to CSV expectations.
      // We look at the next CSV row to know how many teams come "FromPreviousRound".
      // Those slots will be created as QUALIFIER placeholders and must be replaced by winners after propagation.
      List<int[]> qualifierSlots = new ArrayList<>(); // each entry: [gameIndex, side(0=A,1=B)]
      if (i + 1 < rows.size()) {
        String[] nextRow   = rows.get(i + 1);
        Stage    nextStage = Stage.valueOf(stringValue(nextRow, "Round").toUpperCase());
        Round nextRound = built.stream()
                               .filter(r -> r.getStage() == nextStage)
                               .findFirst()
                               .orElse(null);
        if (nextRound != null) {
          int expectedFromPrev = intValue(nextRow, "FromPreviousRound");
          // Fill A then B with QUALIFIER placeholders until we've placed expectedFromPrev entries.
          for (int gi2 = 0; gi2 < nextRound.getGames().size(); gi2++) {
            Game ng = nextRound.getGames().get(gi2);
            if (ng.getTeamA() == null && expectedFromPrev > 0) {
              ng.setTeamA(PlayerPair.qualifier());
              qualifierSlots.add(new int[]{gi2, 0});
              expectedFromPrev--;
              if (expectedFromPrev == 0) {
                break;
              }
            }
            if (ng.getTeamB() == null && expectedFromPrev > 0) {
              ng.setTeamB(PlayerPair.qualifier());
              qualifierSlots.add(new int[]{gi2, 1});
              expectedFromPrev--;
              if (expectedFromPrev == 0) {
                break;
              }
            }
          }
          // Sanity: if next round structure hasn't enough free slots, we don't fail here;
          // fallback propagation will still try to use any remaining empty slots.
        }
      }

      // Propagate winners to the next round and verify numbers
      builder.propagateWinners(t);
      Round nextRound = built.get(built.indexOf(currentRound) + 1);
      long nonNullTeamsNext = nextRound.getGames().stream()
                                       .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                       .filter(Objects::nonNull)
                                       .count();
      int expectedWinners = matches + defaultQualif;
      assertEquals(expectedWinners, nonNullTeamsNext,
                   "Propagation mismatch from " + stage + " to " + nextRound.getStage() + " for " + tournamentId);

      // Verify that qualifier placeholders were actually replaced by real winners (non-BYE, non-QUALIFIER)
      if (!qualifierSlots.isEmpty()) {
        for (int[] pos : qualifierSlots) {
          Game       ng     = nextRound.getGames().get(pos[0]);
          PlayerPair placed = (pos[1] == 0) ? ng.getTeamA() : ng.getTeamB();
          // Must be non-null and not a placeholder anymore
          Assertions.assertFalse(placed == null || placed.isBye() || placed.getType() == PairType.QUALIFIER,
                                 "Expected winner to replace QUALIFIER placeholder at game " + pos[0] + " side " + (pos[1] == 0
                                                                                                                    ? "A"
                                                                                                                    : "B")
                                 + " in " + nextRound.getStage() + " for " + tournamentId);
        }
      }

      // Cross-check composition: Total pairs = FromPreviousRound + NewTeams + BYE + DefaultQualif
      int fromPrev = intValue(row, "FromPreviousRound");
      int newTeams = intValue(row, "NewTeams");
      assertEquals(TotalPairs, fromPrev + newTeams + byeEntries + defaultQualif,
                   "Total pairs composition mismatch in " + stage + " for " + tournamentId);
    }
  }
}
