package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.TeamSide;
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

// @todo testing generateManualRound method
public class QualifyMainRoundGeneratorTest {

  private static Stream<Arguments> fullTournamentCases() {
    return Stream.of(
        Arguments.of(
            32, // preQualDraw
            8,  // numQualifiers
            16, // mainDrawSize
            List.of(Stage.Q1, Stage.Q2, Stage.R16, Stage.QUARTERS, Stage.SEMIS, Stage.FINAL)
        )
    );
  }

  // --- Pré-qualif : 1, 2, 3 tours (Q1..Qk en ordre croissant) ---
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
  })
  void preQual_generateManualRound_builds_expected_stage_and_games(int preQualDraw, int numQualifiers,
                                                                   String expectedStagesCsv) {
    TournamentFormatConfig cfg = TournamentFormatConfig.builder().preQualDrawSize(preQualDraw)
                                                       .nbQualifiers(numQualifiers).mainDrawSize(32).nbSeeds(8).build();

    // Les moins bien classés commencent en pré-qualif: on génère des seeds élevés (ex: 100+)
    List<PlayerPair> pairs = new ArrayList<>();
    for (int i = 0; i < preQualDraw; i++) {
      pairs.add(TestFixtures.buildPairWithSeed(100 + i));
    }

    QualifyMainRoundGenerator gen = new QualifyMainRoundGenerator(cfg.getNbSeeds());

    // Construire la structure des tours (Q1..Qk + main) une seule fois
    Tournament t = new Tournament();
    t.setConfig(cfg);
    List<Round> rounds = gen.createRoundsStructure(t);
    t.getRounds().clear();
    t.getRounds().addAll(rounds);

    String[] expectedStages = expectedStagesCsv.replace(" ", "").split(",");

    List<PlayerPair> currentPairs = pairs;
    for (String expectedStage : expectedStages) {
      // Récupérer le round correspondant dans le tournoi
      Round round = t.getRounds().stream()
                     .filter(r -> r.getStage() != null && expectedStage.equals(r.getStage().name()))
                     .findFirst()
                     .orElseThrow(() -> new AssertionError("Round avec stage " + expectedStage + " introuvable"));

      assertNotNull(round, "Le round de pré-qualif ne doit pas être null");
      assertEquals(expectedStage, round.getStage().name(), "Stage Q attendu");

      int expectedGames = currentPairs.size() / 2;
      assertEquals(expectedGames, round.getGames().size(), "Nombre de matchs attendu sur ce tour de qualifs");

      // Placer les équipes manuellement (A,B),(C,D)... dans le round existant
      for (int i = 0; i < currentPairs.size(); i++) {
        PlayerPair p       = currentPairs.get(i);
        int        gameIdx = i / 2;
        if (i % 2 == 0) {
          round.getGames().get(gameIdx).setTeamA(p);
        } else {
          round.getGames().get(gameIdx).setTeamB(p);
        }
      }

      // Simuler les matchs: la paire au seed le plus bas gagne
      List<PlayerPair> winners = new ArrayList<>();
      round.getGames().forEach(g -> {
        g.setFormat(TestFixtures.createSimpleFormat(1));
        PlayerPair a = g.getTeamA();
        PlayerPair b = g.getTeamB();
        assertNotNull(a, "teamA ne doit pas être null en pré-qualif");
        assertNotNull(b, "teamB ne doit pas être null en pré-qualif");
        PlayerPair winner = (a.getSeed() <= b.getSeed()) ? a : b;
        g.setScore(TestFixtures.createScoreWithWinner(g, winner));
        winners.add(winner);
      });

      // Vérifier cohérence
      assertEquals(expectedGames, winners.size(), "Le tour doit produire autant de vainqueurs que de matchs");

      boolean allLowRank = round.getGames().stream()
                                .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                                .allMatch(p -> p != null && p.getSeed() >= 100);
      assertTrue(allLowRank, "Les pré-qualifs doivent contenir uniquement des équipes moins bien classées");

      // Les vainqueurs deviennent les entrants du tour suivant
      currentPairs = winners;
    }
  }

  @ParameterizedTest(name = "preQual={0}, qualifiers={1}, mainDraw={2} ⇒ stages={3}")
  @MethodSource("fullTournamentCases")
  void fullTournamentStructure_builds_expected_stages(int preQualDraw, int numQualifiers, int mainDrawSize,
                                                      List<Stage> expectedStages) {
    TournamentFormatConfig cfg = TournamentFormatConfig.builder().preQualDrawSize(preQualDraw)
                                                       .nbQualifiers(numQualifiers).mainDrawSize(mainDrawSize).nbSeeds(4).build();

    // Paires moins bien classées pour les qualifs
    List<PlayerPair> preQualPairs = new ArrayList<>();
    for (int i = 0; i < preQualDraw; i++) {
      preQualPairs.add(TestFixtures.buildPairWithSeed(100 + i));
    }

    // Paires mieux classées pour compléter le main draw
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

    assertEquals(expectedStages.size(), t.getRounds().size());
    // On part des pré-qualifs
    List<PlayerPair> currentPairs = preQualPairs;

    for (Stage expectedStage : expectedStages) {
      // Lors du premier stage du main draw, on ajoute les top seeds AVANT de calculer expectedGames
      if (expectedStage == firstMainStage) {
        currentPairs.addAll(mainPairs);
      }

      Round round = t.getRounds().stream()
                     .filter(r -> r.getStage() == expectedStage)
                     .findFirst()
                     .orElseThrow(() -> new AssertionError("Round avec stage " + expectedStage + " introuvable"));

      assertNotNull(round, "Le round " + expectedStage + " ne doit pas être null");

      int expectedGames = currentPairs.size() / 2;
      assertEquals(expectedGames, round.getGames().size(),
                   "Nombre de matchs attendu au stage " + expectedStage);

      // Placer les équipes dans le round
      for (int i = 0; i < currentPairs.size(); i++) {
        PlayerPair p       = currentPairs.get(i);
        int        gameIdx = i / 2;
        if (i % 2 == 0) {
          round.getGames().get(gameIdx).setTeamA(p);
        } else {
          round.getGames().get(gameIdx).setTeamB(p);
        }
      }

      // Simuler les matchs: seed le plus bas gagne
      List<PlayerPair> winners = new ArrayList<>();
      round.getGames().forEach(g -> {
        g.setFormat(TestFixtures.createSimpleFormat(1));
        PlayerPair a = g.getTeamA();
        PlayerPair b = g.getTeamB();
        assertNotNull(a, "teamA ne doit pas être null au stage " + expectedStage);
        assertNotNull(b, "teamB ne doit pas être null au stage " + expectedStage);
        PlayerPair winner = (a.getSeed() <= b.getSeed()) ? a : b;
        g.setScore(TestFixtures.createScoreWithWinner(g, winner));
        winners.add(winner);
      });

      // Les vainqueurs deviennent les entrants du prochain stage
      currentPairs = winners;
    }

    // Vérif: il ne doit rester qu'un seul vainqueur à la fin
    assertEquals(1, currentPairs.size(), "Le tournoi doit produire un seul champion à la fin");
  }

  @Test
  void verifyManualRoundGenerationAndStagesUpToR32() {
    // Configuration du tournoi
    int preQualDrawSize = 16; // Taille du tableau de pré-qualification
    int mainDrawSize    = 32;    // Taille du tableau principal
    int nbQualifiers    = 4;     // Nombre de qualifiés
    int totalPairs      = 40;      // Total des paires dans le tournoi

    // Création de la configuration pour le tournoi
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(8) // Nombre de têtes de série
                                                       .build();

    // Liste de toutes les paires
    List<PlayerPair> allPairs = TestFixtures.createPairs(totalPairs);

    // Initialisation du générateur et du tournoi
    QualifyMainRoundGenerator generator  = new QualifyMainRoundGenerator(cfg.getNbSeeds(), mainDrawSize, nbQualifiers);
    Tournament                tournament = new Tournament();
    tournament.setConfig(cfg);

    // Génération manuelle du tour Q1
    Round       q1Round = generator.generateManualRound(allPairs);
    List<Round> rounds  = generator.createRoundsStructure(tournament);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(rounds);
    tournament.getRounds().replaceAll(round -> round.getStage() == Stage.Q1 ? q1Round : round);

    // Vérification Q1
    assertNotNull(q1Round, "Le round Q1 généré manuellement ne doit pas être null");
    assertEquals(Stage.Q1, q1Round.getStage(), "Le stage du round doit être Q1");
    assertFalse(q1Round.getGames().isEmpty(), "Les matchs du round Q1 ne doivent pas être vides");

    // Ajout des scores pour les matchs de Q1
    q1Round.getGames().forEach(game -> {
      game.setFormat(new MatchFormat());
      PlayerPair winner = game.getTeamA() != null ? game.getTeamA() : game.getTeamB();
      if (winner != null) {
        game.setScore(TestFixtures.createScoreWithWinner(game, winner));
      }
    });

    // Propager les vainqueurs de Q1 vers Q2
    generator.propagateWinners(tournament);

    // Vérification Q2
    Round q2Round = tournament.getRounds().stream()
                              .filter(round -> round.getStage() == Stage.Q2)
                              .findFirst()
                              .orElse(null);

    assertNotNull(q2Round, "Le round Q2 généré automatiquement ne doit pas être null");
    assertFalse(q2Round.getGames().isEmpty(), "Les matchs du round Q2 ne doivent pas être vides");

    List<PlayerPair> q1Winners = q1Round.getGames().stream()
                                        .map(game -> game.getWinnerSide() == TeamSide.TEAM_A ? game.getTeamA() : game.getTeamB())
                                        .filter(Objects::nonNull)
                                        .toList();

    List<PlayerPair> q2Teams = q2Round.getGames().stream()
                                      .flatMap(game -> Stream.of(game.getTeamA(), game.getTeamB()))
                                      .toList();

    assertTrue(q2Teams.containsAll(q1Winners), "Tous les gagnants de Q1 doivent être propagés dans Q2");

    // Ajout des scores pour les matchs de Q2
    q2Round.getGames().forEach(game -> {
      PlayerPair winner = game.getTeamA() != null ? game.getTeamA() : game.getTeamB();
      if (winner != null) {
        game.setScore(TestFixtures.createScoreWithWinner(game, winner));
      }
    });

    // Propager les vainqueurs de Q2 vers R32
    generator.propagateWinners(tournament);

    // Vérification R32
    Round r32 = tournament.getRounds().stream()
                          .filter(round -> round.getStage() == Stage.R32)
                          .findFirst()
                          .orElse(null);

    assertNotNull(r32, "Le round R32 généré automatiquement ne doit pas être null");
    assertFalse(r32.getGames().isEmpty(), "Les matchs du round R32 ne doivent pas être vides");

    List<PlayerPair> q2Winners = q2Round.getGames().stream()
                                        .map(game -> game.getWinnerSide() == TeamSide.TEAM_A ? game.getTeamA() : game.getTeamB())
                                        .filter(Objects::nonNull)
                                        .toList();

    List<PlayerPair> r32Teams = r32.getGames().stream()
                                   .flatMap(game -> Stream.of(game.getTeamA(), game.getTeamB()))
                                   .toList();

    assertTrue(r32Teams.containsAll(q2Winners), "Tous les gagnants de Q2 doivent être propagés dans R32");

    // Vérification des matchs dans R32
    long matchesWithBye = r32.getGames().stream()
                             .filter(game -> game.getTeamA() == null || game.getTeamB() == null)
                             .count();

    long matchesWithoutBye = r32.getGames().stream()
                                .filter(game -> game.getTeamA() != null && game.getTeamB() != null)
                                .count();

    assertEquals(8, matchesWithoutBye, "Il doit y avoir 8 matchs normaux sans BYE dans R32");
    assertEquals(0, matchesWithBye, "Il ne doit pas y avoir de matchs avec BYE dans R32");

    // Vérifications supplémentaires
    assertEquals(8, q1Round.getGames().size(), "Le round Q1 doit avoir 8 matchs au total");
    assertEquals(4, q2Round.getGames().size(), "Le round Q2 doit avoir 4 matchs au total");
    assertEquals(8, r32.getGames().size(), "Le round R32 doit avoir 8 matchs au total");
  }

}