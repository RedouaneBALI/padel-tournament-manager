package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test suite for {@link QualifyMainRoundGenerator} covering both phases: 1) Qualification brackets (Q1..Qk) 2) Main draw (R{2^n}..FINAL)
 *
 * Goals: - Structure of pre‑qual rounds matches the expected ascending stages (Q1, Q2, ...) - Manual round population is supported and deterministic
 * scoring works - Propagation from last qualification round to the first main round fills the slots correctly - When qualifications are not finished,
 * main draw rounds may exist structurally but must have null teams/scores (placeholders) - Full run from pre‑qual to champion produces exactly one
 * winner
 */
public class QualifyMainRoundGeneratorTest {

  // ---------- Data Providers ----------

  private static Stream<Arguments> fullTournamentCases() {
    return Stream.of(
        Arguments.of(
            32, // preQualDraw
            8,  // numQualifiers
            16, // mainDrawSize
            List.of(Stage.Q1, Stage.Q2, Stage.R16, Stage.QUARTERS, Stage.SEMIS, Stage.FINAL)
        ),
        Arguments.of(
            16, // preQualDraw
            4,  // numQualifiers
            32, // mainDrawSize
            List.of(Stage.Q1, Stage.Q2, Stage.R32, Stage.R16, Stage.QUARTERS, Stage.SEMIS, Stage.FINAL)
        )
    );
  }

  // ---------- Qualification structure only ----------

  private static Round roundByName(Tournament t, String stageName) {
    return t.getRounds().stream()
            .filter(r -> r.getStage() != null && stageName.equals(r.getStage().name()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Round avec stage " + stageName + " introuvable"));
  }

  // ---------- Full path: Q... -> Main draw -> Champion ----------

  private static void placePairsAB(Round round, List<PlayerPair> pairs) {
    for (int i = 0; i < pairs.size(); i++) {
      PlayerPair p       = pairs.get(i);
      int        gameIdx = i / 2;
      if (i % 2 == 0) {
        round.getGames().get(gameIdx).setTeamA(p);
      } else {
        round.getGames().get(gameIdx).setTeamB(p);
      }
    }
  }

  // ---------- Placeholder behavior when Q not finished ----------

  private static List<PlayerPair> finishRoundLowestSeedWins(Round round) {
    List<PlayerPair> winners = new ArrayList<>();
    round.getGames().forEach(g -> {
      g.setFormat(TestFixtures.createSimpleFormat(1));
      PlayerPair a = g.getTeamA();
      PlayerPair b = g.getTeamB();
      assertNotNull(a, "teamA ne doit pas être null pour ce match");
      assertNotNull(b, "teamB ne doit pas être null pour ce match");
      PlayerPair winner = (a.getSeed() <= b.getSeed()) ? a : b;
      g.setScore(TestFixtures.createScoreWithWinner(g, winner));
      winners.add(winner);
    });
    return winners;
  }

  // ---------- Helpers ----------

  // preQualDraw, numQualifiers -> expected ascending Q-stages sequence
  @ParameterizedTest(name = "preQual={0}, qualifiers={1} ⇒ stages(asc)={2}")
  @CsvSource({
      // preQualDraw, numQualifiers, expectedStages (ascending order)
      "4,2,'Q1'",
      "4,1,'Q1,Q2'",
      "8,2,'Q1,Q2'",
      "16,4,'Q1,Q2'",
      "16,8,'Q1'",
      "32,16,'Q1'",
      "32,8,'Q1,Q2'",
      "32,4,'Q1,Q2,Q3'"
  })
  void preQual_generateManualRound_builds_expected_stage_and_games(int preQualDraw, int numQualifiers,
                                                                   String expectedStagesCsv) {
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDraw)
                                                       .nbQualifiers(numQualifiers)
                                                       .mainDrawSize(32)
                                                       .nbSeeds(8)
                                                       .build();

    // Least‑ranked pairs play the qualifications: create artificially high seeds (100+)
    List<PlayerPair> pairs = new ArrayList<>();
    for (int i = 0; i < preQualDraw; i++) {
      pairs.add(TestFixtures.buildPairWithSeed(100 + i));
    }

    QualifyMainRoundGenerator gen = new QualifyMainRoundGenerator(cfg.getNbSeeds());

    // Build once the tournament rounds structure (Q1..Qk + main)
    Tournament t = new Tournament();
    t.setConfig(cfg);
    List<Round> rounds = gen.createRoundsStructure(t);
    t.getRounds().clear();
    t.getRounds().addAll(rounds);

    String[] expectedStages = expectedStagesCsv.replace(" ", "").split(",");

    List<PlayerPair> currentPairs = pairs;
    for (String expectedStage : expectedStages) {
      Round round = roundByName(t, expectedStage);

      int expectedGames = currentPairs.size() / 2;
      assertEquals(expectedGames, round.getGames().size(),
                   "Nombre de matchs attendu sur le tour de qualifs " + expectedStage);

      // Place teams manually (A,B),(C,D)... into existing round
      placePairsAB(round, currentPairs);

      // Simulate matches: lower seed wins (deterministic)
      List<PlayerPair> winners = finishRoundLowestSeedWins(round);

      // Sanity checks
      assertEquals(expectedGames, winners.size(),
                   "Le tour doit produire autant de vainqueurs que de matchs");
      assertTrue(round.getGames().stream()
                      .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                      .allMatch(p -> p != null && p.getSeed() >= 100),
                 "Les pré‑qualifs doivent contenir uniquement des équipes moins bien classées");

      // Winners go to next Q-stage (or main)
      currentPairs = winners;
    }
  }

  @ParameterizedTest(name = "preQual={0}, qualifiers={1}, mainDraw={2} ⇒ stages={3}")
  @MethodSource("fullTournamentCases")
  void fullTournamentStructure_builds_expected_stages(int preQualDraw, int numQualifiers, int mainDrawSize,
                                                      List<Stage> expectedStages) {
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDraw)
                                                       .nbQualifiers(numQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(4)
                                                       .build();

    // Less‑ranked pairs for qualifications
    List<PlayerPair> preQualPairs = new ArrayList<>();
    for (int i = 0; i < preQualDraw; i++) {
      preQualPairs.add(TestFixtures.buildPairWithSeed(100 + i));
    }

    // Better pairs to complete the main draw (seeds 1..)
    List<PlayerPair> mainPairs = new ArrayList<>();
    for (int i = 0; i < (mainDrawSize - numQualifiers); i++) {
      mainPairs.add(TestFixtures.buildPairWithSeed(i + 1));
    }

    QualifyMainRoundGenerator gen = new QualifyMainRoundGenerator(cfg.getNbSeeds());

    Tournament t = new Tournament();
    t.setConfig(cfg);
    List<Round> rounds = gen.createRoundsStructure(t);
    t.getRounds().clear();
    t.getRounds().addAll(rounds);

    Stage firstMainStage = Stage.fromNbTeams(mainDrawSize);

    assertEquals(expectedStages.size(), t.getRounds().size(),
                 "Structure globale inattendue (nb de rounds)");

    List<PlayerPair> currentPairs = preQualPairs; // start from Q

    for (Stage expectedStage : expectedStages) {
      // When we reach first main stage, add the top seeded mainPairs BEFORE computing expectedGames
      if (expectedStage == firstMainStage) {
        currentPairs.addAll(mainPairs);
      }

      Round round = t.getRounds().stream()
                     .filter(r -> r.getStage() == expectedStage)
                     .findFirst()
                     .orElseThrow(() -> new AssertionError("Round avec stage " + expectedStage + " introuvable"));

      int expectedGames = currentPairs.size() / 2;
      assertEquals(expectedGames, round.getGames().size(),
                   "Nombre de matchs attendu au stage " + expectedStage);

      // Populate and finish deterministically
      placePairsAB(round, currentPairs);
      List<PlayerPair> winners = finishRoundLowestSeedWins(round);

      // Winners move forward
      currentPairs = winners;
    }

    // At the end, exactly one champion must remain
    assertEquals(1, currentPairs.size(), "Le tournoi doit produire un seul champion à la fin");
  }

  @Test
  void mainDrawRoundsRemainPlaceholders_untilQualificationsFinished() {
    int preQualDrawSize = 8;
    int mainDrawSize    = 16;
    int nbQualifiers    = 4;

    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(4)
                                                       .build();

    // Build Q pairs only (do not touch main pairs yet)
    List<PlayerPair> qPairs = new ArrayList<>();
    for (int i = 0; i < preQualDrawSize; i++) {
      qPairs.add(TestFixtures.buildPairWithSeed(100 + i));
    }
    List<PlayerPair> mainPairs = new ArrayList<>();
    for (int i = 0; i < (mainDrawSize - nbQualifiers); i++) {
      mainPairs.add(TestFixtures.buildPairWithSeed(i + 1));
    }
    List<PlayerPair> allPairs = new ArrayList<>();
    allPairs.addAll(mainPairs);
    allPairs.addAll(qPairs);

    QualifyMainRoundGenerator gen = new QualifyMainRoundGenerator(cfg.getNbSeeds());

    Tournament t = new Tournament();
    t.setConfig(cfg);
    List<Round> rounds = gen.createRoundsStructure(t);
    t.getRounds().clear();
    t.getRounds().addAll(rounds);
    Round q1 = gen.generateManualRound(t, allPairs);
    t.getRounds().replaceAll(r -> r.getStage() == Stage.Q1 ? q1 : r);

    // Put teams and finish all but the first match
    placePairsAB(q1, qPairs);
    for (int i = 1; i < q1.getGames().size(); i++) {
      var        g      = q1.getGames().get(i);
      PlayerPair a      = g.getTeamA();
      PlayerPair b      = g.getTeamB();
      PlayerPair winner = (a.getSeed() <= b.getSeed()) ? a : b;
      g.setFormat(TestFixtures.createSimpleFormat(1));
      g.setScore(TestFixtures.createScoreWithWinner(g, winner));
    }

    // Propagate now while Q not finished
    gen.propagateWinners(t);

    // All main draw rounds should exist structurally, but their matches must be placeholders (null teams, null score)
    List<Round> mainRounds = t.getRounds().stream()
                              .filter(r -> r.getStage() != Stage.Q1 && r.getStage() != Stage.Q2) // keep it generic if only two Q stages max used here
                              .filter(r -> r.getStage() != Stage.GROUPS) // no groups in this format
                              .toList();

    assertTrue(mainRounds.size() >= 1, "Des rounds du tableau final doivent exister structurellement");

    // Identify the first main-draw stage (e.g., R16 when mainDrawSize=16)
    Stage firstMainStageId = Stage.fromNbTeams(mainDrawSize);
    Round firstMainStage = t.getRounds().stream()
                            .filter(r -> r.getStage() == firstMainStageId)
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("First main stage not found"));

    // In the first main round:
    //  - direct entrants (mainDrawSize - nbQualifiers) should already be placed (non-null)
    //  - slots for future qualifiers should be left null (exactly nbQualifiers null teams across A/B)
    //  - BYEs (if any for other configs) are allowed and count as non-null teams
    long totalTeamsFirst = firstMainStage.getGames().stream()
                                         .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                         .count();
    assertEquals(mainDrawSize, totalTeamsFirst, "Le premier round du tableau doit contenir exactement mainDrawSize emplacements");

    long nullTeamsFirst = firstMainStage.getGames().stream()
                                        .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                        .filter(Objects::isNull)
                                        .count();
    assertEquals(nbQualifiers, nullTeamsFirst, "Le premier round doit comporter exactement nbQualifiers emplacements null en attente des qualifiés");

    // All scores in the first main round must still be null at this point
    assertTrue(firstMainStage.getGames().stream().allMatch(g -> g.getScore() == null),
               "Aucun score ne doit être défini dans le premier round du tableau tant que les qualifs ne sont pas finies");

    // For subsequent main-draw rounds (e.g., QUARTERS/SEMIS/FINAL), all teams should still be null and scores null
    List<Round> laterMainRounds = mainRounds.stream()
                                            .filter(r -> r.getStage() != firstMainStageId)
                                            .toList();

    assertAll("Les rounds suivants du tableau doivent rester des placeholders complets",
              laterMainRounds.stream().map(r -> () -> assertAll(
                  "Round " + r.getStage(),
                  r.getGames().stream().map(g -> () -> {
                    assertNull(g.getTeamA(), "teamA doit être null avant la complétion des qualifs");
                    assertNull(g.getTeamB(), "teamB doit être null avant la complétion des qualifs");
                    assertNull(g.getScore(), "score doit être null avant la complétion des qualifs");
                  })
              ))
    );

    // Now finish the remaining Q1 game and propagate again
    var        g0 = q1.getGames().get(0);
    PlayerPair a0 = g0.getTeamA();
    PlayerPair b0 = g0.getTeamB();
    PlayerPair w0 = (a0.getSeed() <= b0.getSeed()) ? a0 : b0;
    g0.setFormat(TestFixtures.createSimpleFormat(1));
    g0.setScore(TestFixtures.createScoreWithWinner(g0, w0));

    gen.propagateWinners(t);

    // After completion, at least the first main stage should be populated (non-null teams)
    Stage firstMain      = Stage.fromNbTeams(mainDrawSize);
    Round firstMainRound = t.getRounds().stream().filter(r -> r.getStage() == firstMain).findFirst().orElseThrow();

    long nonNullTeams = firstMainRound.getGames().stream()
                                      .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                      .filter(Objects::nonNull)
                                      .count();

    assertTrue(nonNullTeams > 0, "Des équipes doivent être injectées dans le premier round du tableau final après fin des qualifs");
  }
}