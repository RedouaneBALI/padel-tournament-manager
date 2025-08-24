package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.Comparator;
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

  /**
   * Returns linear indices (0..mainDrawSize-1) of qualifier-reserved slots in the first main round.
   */
  private static List<Integer> qualifierSlotIndices(Round round) {
    List<Integer> out = new ArrayList<>();
    int           idx = 0;
    for (var g : round.getGames()) {
      var a = g.getTeamA();
      var b = g.getTeamB();
      if (a == null || (a.getType() != null && a.getType() == PairType.QUALIFIER)) {
        out.add(idx);
      }
      idx++;
      if (b == null || (b.getType() != null && b.getType() == PairType.QUALIFIER)) {
        out.add(idx);
      }
      idx++;
    }
    return out;
  }

  /**
   * True if there is at least one slot in first half and one slot in second half of the bracket.
   */
  private static boolean spansBothHalves(List<Integer> positions, int totalSlots) {
    if (positions.size() < 2) {
      return true; // cannot test spread with a single qualifier
    }
    boolean firstHalf  = positions.stream().anyMatch(i -> i < (totalSlots / 2));
    boolean secondHalf = positions.stream().anyMatch(i -> i >= (totalSlots / 2));
    return firstHalf && secondHalf;
  }

  /**
   * Guards against extreme clustering: all qualifiers in first or last quartile.
   */
  private static boolean notAllInSingleQuartile(List<Integer> positions, int totalSlots) {
    if (positions.size() < 2) {
      return true;
    }
    int     q               = Math.max(1, totalSlots / 4);
    boolean allInFirstQuart = positions.stream().allMatch(i -> i < q);
    boolean allInLastQuart  = positions.stream().allMatch(i -> i >= (totalSlots - q));
    return !(allInFirstQuart || allInLastQuart);
  }

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
  // @todo à déplacer
  private static Round roundByName(Tournament t, String stageName) {
    return t.getRounds().stream()
            .filter(r -> r.getStage() != null && stageName.equals(r.getStage().name()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Round avec stage " + stageName + " introuvable"));
  }

  // ---------- Full path: Q... -> Main draw -> Champion ----------

  private static void placePairsAB(Round round, List<PlayerPair> pairs) {
    int requiredTeams = round.getGames().size() * 2;

    // Build a working list we can pad/truncate
    List<PlayerPair> src = new ArrayList<>(pairs != null ? pairs : List.of());

    // Pad with BYE if we don't have enough
    while (src.size() < requiredTeams) {
      src.add(PlayerPair.bye());
    }
    // Truncate if we have too many
    if (src.size() > requiredTeams) {
      src = src.subList(0, requiredTeams);
    }

    // Assign sequentially: (A,B),(C,D),...
    int idx = 0;
    for (var g : round.getGames()) {
      g.setTeamA(src.get(idx++));
      g.setTeamB(src.get(idx++));
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
  @ParameterizedTest(name = "preQual={0}, qualifiers={1}, mainDraw={2} ⇒ stages(asc)={3}")
  @CsvSource({
      // preQualDraw, numQualifiers, mainDrawSize, expectedStages (ascending order)
      "4,2,32,'Q1'",
      "4,1,32,'Q1,Q2'",
      "8,2,32,'Q1,Q2'",
      "16,4,32,'Q1,Q2'",
      "16,8,32,'Q1'",
      "32,16,32,'Q1'",
      "32,8,32,'Q1,Q2'",
      "32,4,32,'Q1,Q2,Q3'"
  })
  void preQual_generateManualRounds_builds_expected_stage_and_games(int preQualDraw, int numQualifiers,
                                                                    int mainDrawSize,
                                                                    String expectedStagesCsv) {
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDraw)
                                                       .nbQualifiers(numQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(8)
                                                       .build();

    // Least‑ranked pairs play the qualifications: create artificially high seeds (100+)
    List<PlayerPair> pairs = new ArrayList<>();
    for (int i = 0; i < mainDrawSize + preQualDraw; i++) {
      pairs.add(TestFixtures.buildPairWithSeed(100 + i));
    }

    QualifyMainRoundGenerator
        gen =
        new QualifyMainRoundGenerator(cfg.getNbSeeds(), cfg.getMainDrawSize(), cfg.getNbQualifiers(), cfg.getPreQualDrawSize());

    // Build once the tournament rounds structure (Q1..Qk + main)
    Tournament t = new Tournament();
    t.setConfig(cfg);
    List<Round> rounds = gen.generateManualRounds(pairs);
    t.getRounds().addAll(rounds);

    String[] expectedStages = expectedStagesCsv.replace(" ", "").split(",");

    List<PlayerPair> currentQualifPairs = pairs.subList(pairs.size() - preQualDraw, pairs.size()); // à modifier
    for (String expectedStage : expectedStages) {
      Round round = roundByName(t, expectedStage);

      int expectedGames = currentQualifPairs.size() / 2;
      assertEquals(expectedGames, round.getGames().size(),
                   "Nombre de matchs attendu sur le tour de qualifs " + expectedStage);

      // Place teams manually (A,B),(C,D)... into existing round
      placePairsAB(round, currentQualifPairs);

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
      currentQualifPairs = winners;
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

    QualifyMainRoundGenerator
        gen =
        new QualifyMainRoundGenerator(cfg.getNbSeeds(), cfg.getMainDrawSize(), cfg.getNbQualifiers(), cfg.getPreQualDrawSize());

    Tournament t = new Tournament();
    t.setConfig(cfg);
    // Build complete structure (qualifications + main draw)
    List<PlayerPair> allPairs = new ArrayList<>();
    allPairs.addAll(mainPairs);
    allPairs.addAll(preQualPairs);
    List<Round> rounds = gen.generateManualRounds(allPairs);
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

    QualifyMainRoundGenerator
        gen =
        new QualifyMainRoundGenerator(cfg.getNbSeeds(), cfg.getMainDrawSize(), cfg.getNbQualifiers(), cfg.getPreQualDrawSize());

    Tournament t = new Tournament();
    t.setConfig(cfg);
    List<Round> rounds = gen.generateManualRounds(allPairs);
    t.getRounds().addAll(rounds);
    Round q1 = rounds.getFirst();

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

  @ParameterizedTest(name = "algo gen: preQual={0}, nbQualifiers={1}, main={2}, seeds={3}")
  @CsvSource({
      // preQualDrawSize, nbQualifiers, mainDrawSize, nbSeeds, expectedQStages
      "8,2,32,8,'Q1,Q2'",
      "8,4,32,8,'Q1'",
      "16,4,32,8,'Q1,Q2'",
      "4,1,16,4,'Q1,Q2'",
      "4,2,16,4,'Q1'"
  })
  void generateAlgorithmicRounds_buildsQualisAndMain_withSeedsSeparated(
      int preQualDrawSize,
      int nbQualifiers,
      int mainDrawSize,
      int nbSeeds,
      String expectedQStagesCsv
  ) {
    // Config du tournoi
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .mainDrawSize(mainDrawSize)
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .nbSeeds(nbSeeds)
                                                       .build();

    Tournament tournament = new Tournament();
    tournament.setConfig(cfg);

    int directEntrantsCount = Math.max(0, mainDrawSize - nbQualifiers);
    int totalPairs          = directEntrantsCount + preQualDrawSize;

    // Crée une liste de paires où les meilleures seeds sont en premier
    List<PlayerPair> allPairs = TestFixtures.createPairs(totalPairs);
    allPairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    // Générateur (algo)
    QualifyMainRoundGenerator gen = new QualifyMainRoundGenerator(nbSeeds, mainDrawSize, nbQualifiers, preQualDrawSize);

    // Génération
    List<Round> rounds = gen.generateAlgorithmicRounds(allPairs);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(rounds);

    // --- Vérifs QUALIFS ---
    String[] expectedQStages = expectedQStagesCsv.split(",");
    for (String s : expectedQStages) {
      Stage qs = Stage.valueOf(s.trim());
      Round qr = tournament.getRoundByStage(qs);
      assertNotNull(qr, "Le round de qualification " + qs + " doit exister");
    }
    if (expectedQStages.length > 0) {
      Stage q1 = Stage.valueOf(expectedQStages[0].trim());
      Round r1 = tournament.getRoundByStage(q1);
      assertEquals(preQualDrawSize / 2, r1.getGames().size(), "Nombre de matchs incorrect pour Q1");
      if (expectedQStages.length >= 2) {
        Stage q2 = Stage.valueOf(expectedQStages[1].trim());
        Round r2 = tournament.getRoundByStage(q2);
        assertEquals(preQualDrawSize / 4, r2.getGames().size(), "Nombre de matchs incorrect pour Q2");
      }
    }

    // --- Vérifs MAIN DRAW ---
    Stage firstMainStage = Stage.fromNbTeams(mainDrawSize);
    Round mainFirst      = tournament.getRoundByStage(firstMainStage);
    assertNotNull(mainFirst, "Le premier round du tableau principal doit exister");
    assertEquals(mainDrawSize / 2, mainFirst.getGames().size(), "Nombre de matchs du premier round principal incorrect");

    // Les seeds doivent être présentes dans le main dès le départ
    long seedsInMain = mainFirst.getGames().stream()
                                .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                                .filter(Objects::nonNull)
                                .filter(p -> p.getSeed() >= 1 && p.getSeed() <= nbSeeds)
                                .count();
    assertEquals(Math.min(nbSeeds, directEntrantsCount),
                 seedsInMain,
                 "Toutes les têtes de série directes doivent être placées dans le tableau principal");

    // Pas de seed vs seed au 1er tour
    boolean anySeedVsSeed = mainFirst.getGames().stream().anyMatch(g ->
                                                                       isSeed(g.getTeamA(), nbSeeds) && isSeed(g.getTeamB(), nbSeeds)
    );
    assertFalse(anySeedVsSeed, "Deux têtes de série ne doivent pas s'affronter au premier tour");

    // Emplacements réservés aux qualifiés: soit null, soit PlayerPair de type QUALIFIER
    List<Integer> qPositions = qualifierSlotIndices(mainFirst);
    assertEquals(nbQualifiers, qPositions.size(), "Il doit y avoir exactement nbQualifiers emplacements réservés aux qualifiés");

    // Distribution: s'il y a au moins 2 qualifiés, ils doivent couvrir les deux moitiés du tableau
    int totalSlots = mainDrawSize; // 2 teams per game * (mainDrawSize/2 games)
    if (nbQualifiers >= 2) {
//      assertTrue(spansBothHalves(qPositions, totalSlots),
      //                "Les emplacements qualifiés doivent être répartis: au moins un dans chaque moitié du tableau");
      assertTrue(notAllInSingleQuartile(qPositions, totalSlots),
                 "Les emplacements qualifiés ne doivent pas être tous concentrés dans un seul quart du tableau");
    }

    // Aucune paire de pré-qualif ne doit figurer dans le main tant que les qualifs ne sont pas finies
    List<PlayerPair> preQualSection = allPairs.subList(directEntrantsCount, Math.min(totalPairs, directEntrantsCount + preQualDrawSize));
    boolean preQualFoundInMain = mainFirst.getGames().stream()
                                          .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                                          .filter(Objects::nonNull)
                                          .anyMatch(preQualSection::contains);
    assertFalse(preQualFoundInMain, "Aucune paire de pré-qualifications ne doit être dans le tableau principal avant la fin des qualifs");
  }

  @ParameterizedTest(name = "propagate winners: preQual={0}, nbQualifiers={1}, main={2}")
  @CsvSource({
      // preQualDrawSize, nbQualifiers, mainDrawSize
      "8,2,16",
      "16,4,32"
  })
  void propagateWinners_movesLastQualWinners_intoFirstMainRound(int preQualDrawSize,
                                                                int nbQualifiers,
                                                                int mainDrawSize) {
    int nbSeeds = Math.min(8, Math.max(2, mainDrawSize / 4));

    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(nbSeeds)
                                                       .build();

    // Build pairs: best go to main, worst go to pre-qual
    int directEntrants = Math.max(0, mainDrawSize - nbQualifiers);

    List<PlayerPair> mainPairs = new ArrayList<>();
    for (int i = 0; i < directEntrants; i++) {
      mainPairs.add(TestFixtures.buildPairWithSeed(i + 1));
    }

    List<PlayerPair> preQualPairs = new ArrayList<>();
    for (int i = 0; i < preQualDrawSize; i++) {
      preQualPairs.add(TestFixtures.buildPairWithSeed(100 + i));
    }

    List<PlayerPair> allPairs = new ArrayList<>();
    allPairs.addAll(mainPairs);
    allPairs.addAll(preQualPairs);

    QualifyMainRoundGenerator gen =
        new QualifyMainRoundGenerator(cfg.getNbSeeds(), cfg.getMainDrawSize(), cfg.getNbQualifiers(), cfg.getPreQualDrawSize());

    Tournament t = new Tournament();
    t.setConfig(cfg);
    List<Round> rounds = gen.generateManualRounds(allPairs);
    t.getRounds().addAll(rounds);

    // Identify Q rounds in ascending order (Q1 < Q2 < Q3 ...) by stage name
    List<Round> qRounds = t.getRounds().stream()
                           .filter(r -> r.getStage() != null && r.getStage().isQualification())
                           // Sort by the stage name so that Q1 < Q2 < Q3 regardless of enum ordinal ordering
                           .sorted(Comparator.comparing(r -> r.getStage().name()))
                           .toList();
    assertFalse(qRounds.isEmpty(), "Il doit y avoir au moins un round de qualifications");

    // Defensive: the last qualification round must have exactly nbQualifiers matches
    Round lastQ = qRounds.get(qRounds.size() - 1);
    assertEquals(nbQualifiers, lastQ.getGames().size(), "La taille du dernier tour de qualifs doit être égale au nombre de places qualificatives");

    // Before finishing quals, first main round must have exactly nbQualifiers empty slots
    Stage firstMainStage = Stage.fromNbTeams(mainDrawSize);
    Round firstMain      = t.getRoundByStage(firstMainStage);
    assertNotNull(firstMain, "Le premier round du tableau principal doit exister");

    long nullBefore = firstMain.getGames().stream()
                               .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                               .filter(Objects::isNull)
                               .count();
    assertEquals(nbQualifiers, nullBefore,
                 "Avant propagation finale, le premier round doit comporter nbQualifiers emplacements vides");

    // Play all Q rounds to completion, collecting winners from the **last** qualification round
    List<PlayerPair> current      = new ArrayList<>(preQualPairs);
    List<PlayerPair> lastQWinners = null;
    for (int qi = 0; qi < qRounds.size(); qi++) {
      Round qr = qRounds.get(qi);
      placePairsAB(qr, current);
      lastQWinners = finishRoundLowestSeedWins(qr);
      current      = lastQWinners;
    }
    assertNotNull(lastQWinners, "Les qualifs doivent produire des vainqueurs");
    assertEquals(nbQualifiers, lastQWinners.size(),
                 "Le dernier tour de qualifs doit produire exactement nbQualifiers vainqueurs");

    // Now propagate winners into the main draw
    gen.propagateWinners(t);

    // After propagation: no empty slots should remain in the first main round
    long nullAfter = firstMain.getGames().stream()
                              .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                              .filter(Objects::isNull)
                              .count();
    assertEquals(0, nullAfter,
                 "Après propagation, il ne doit plus rester d'emplacements vides dans le premier round");

    // And the winners of last Q round must be present among the teams of the first main round
    List<PlayerPair> teamsInMain = firstMain.getGames().stream()
                                            .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                            .filter(Objects::nonNull)
                                            .toList();
    for (PlayerPair qWinner : lastQWinners) {
      assertTrue(teamsInMain.contains(qWinner),
                 "Chaque vainqueur des qualifs doit être injecté dans le premier round du tableau principal");
    }

    // Sanity: total non-null teams in first main is mainDrawSize
    long nonNullAfter = teamsInMain.size();
    assertEquals(mainDrawSize, nonNullAfter,
                 "Le premier round doit être complètement rempli après propagation");
  }

  private boolean isSeed(PlayerPair p, int nbSeeds) {
    return p != null && p.getSeed() >= 1 && p.getSeed() <= nbSeeds;
  }
}