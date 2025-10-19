package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests pour la propagation des gagnants entre les rounds, notamment pour les qualifiers.
 */
class WinnerPropagationTest {

  @Test
  void testQualifierPropagation_winnersPlacedInCorrectOrder() {
    // Given: Un tournoi avec Q2 (4 matchs) qui propage vers R32 (4 slots QUALIFIER)
    // Configuration : preQual=16, nbQualifiers=4, mainDraw=32
    // On doit avoir 28 équipes : 12 vont directement en R32, 16 passent par Q1
    Tournament tournament = TestFixtures.makeTournament(16, 4, 32, 8, 4, io.github.redouanebali.model.format.DrawMode.SEEDED);

    // Créer 28 équipes TOUTES avec des seeds (1-28)
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(28);
    // Les équipes sont déjà créées avec seeds 1, 2, 3, ..., 28

    // Générer le draw
    TournamentBuilder.setupAndPopulateTournament(tournament, teams);

    // Récupérer Q1, Q2 et R32
    Round q1Round  = tournament.getRoundByStage(Stage.Q1);
    Round q2Round  = tournament.getRoundByStage(Stage.Q2);
    Round r32Round = tournament.getRoundByStage(Stage.R32);

    assertNotNull(q1Round, "Q1 doit exister");
    assertNotNull(q2Round, "Q2 doit exister");
    assertNotNull(r32Round, "R32 doit exister");

    // DEBUG : Afficher le contenu de Q1
    long teamsInQ1 = q1Round.getGames().stream()
                            .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                            .filter(p -> p != null && !p.isBye() && !p.isQualifier())
                            .count();

    // Vérifier que Q1 contient bien des équipes
    assertTrue(teamsInQ1 > 0, "Q1 doit contenir des équipes après génération du draw");

    // Simuler les résultats de Q1 (8 matchs → 8 gagnants vers Q2)
    // IMPORTANT : Utiliser TestFixtures.createScoreWithWinner() pour créer des scores valides
    for (Game game : q1Round.getGames()) {
      PlayerPair winner = null;
      if (game.getTeamA() != null && !game.getTeamA().isBye()) {
        winner = game.getTeamA();
      } else if (game.getTeamB() != null && !game.getTeamB().isBye()) {
        winner = game.getTeamB();
      }

      if (winner != null) {
        // Créer un score valide avec le gagnant
        game.setScore(TestFixtures.createScoreWithWinner(game, winner));
      }
    }

    // Propager Q1 → Q2 (première propagation)
    TournamentBuilder.propagateWinners(tournament);

    long teamsInQ2 = q2Round.getGames().stream()
                            .flatMap(g -> java.util.stream.Stream.of(g.getTeamA(), g.getTeamB()))
                            .filter(p -> p != null && !p.isBye() && !p.isQualifier())
                            .count();

    // Vérifier que Q2 est rempli
    assertTrue(teamsInQ2 > 0, "Q2 doit avoir des équipes après propagation depuis Q1");

    // Simuler les résultats de Q2 et générer des gagnants avec des scores valides
    List<PlayerPair> q2Winners = new ArrayList<>();
    for (int i = 0; i < q2Round.getGames().size(); i++) {
      Game game = q2Round.getGames().get(i);

      // Utiliser l'équipe A comme gagnante (ou B si A est null/BYE)
      PlayerPair winner = null;
      if (game.getTeamA() != null && !game.getTeamA().isBye()) {
        winner = game.getTeamA();
      } else if (game.getTeamB() != null && !game.getTeamB().isBye()) {
        winner = game.getTeamB();
      }

      if (winner != null) {
        // Créer un score valide pour ce match
        game.setScore(TestFixtures.createScoreWithWinner(game, winner));
        q2Winners.add(winner);
      }
    }

    // Collecter les slots QUALIFIER dans R32 AVANT propagation
    List<QualifierSlotInfo> qualifierSlotsBefore = new ArrayList<>();
    for (int gameIdx = 0; gameIdx < r32Round.getGames().size(); gameIdx++) {
      Game game = r32Round.getGames().get(gameIdx);

      if (game.getTeamA() != null && game.getTeamA().isQualifier()) {
        qualifierSlotsBefore.add(new QualifierSlotInfo(gameIdx, true, "TeamA"));
      }
      if (game.getTeamB() != null && game.getTeamB().isQualifier()) {
        qualifierSlotsBefore.add(new QualifierSlotInfo(gameIdx, false, "TeamB"));
      }
    }

    // When: Propager les gagnants de Q2 vers R32 (deuxième propagation)
    TournamentBuilder.propagateWinners(tournament);

    // Then: Vérifier que chaque gagnant de Q2 est placé dans le bon ordre dans R32
    for (int i = 0; i < q2Winners.size() && i < qualifierSlotsBefore.size(); i++) {
      PlayerPair        expectedWinner = q2Winners.get(i);
      QualifierSlotInfo targetSlot     = qualifierSlotsBefore.get(i);

      Game       r32Game    = r32Round.getGames().get(targetSlot.gameIndex);
      PlayerPair actualTeam = targetSlot.isTeamA ? r32Game.getTeamA() : r32Game.getTeamB();

      assertNotNull(actualTeam,
                    String.format("Le slot R32[%d].%s ne doit pas être null après propagation",
                                  targetSlot.gameIndex, targetSlot.side));

      assertEquals(expectedWinner.getSeed(), actualTeam.getSeed(),
                   String.format("Le gagnant du match Q2-%d (seed %d) doit être placé dans le slot Q%d (R32[%d].%s), " +
                                 "mais on trouve seed %d",
                                 i + 1,
                                 expectedWinner.getSeed(),
                                 i + 1,
                                 targetSlot.gameIndex,
                                 targetSlot.side,
                                 actualTeam.getSeed()));

      assertEquals(expectedWinner.getPlayer1().getName(), actualTeam.getPlayer1().getName(),
                   String.format("Le gagnant du match Q2-%d doit être correctement placé dans le slot Q%d",
                                 i + 1, i + 1));
    }
  }

  @Test
  void testQualifierPropagation_outOfOrderExecution() {
    // Given: Un tournoi avec Q2 → R32
    Tournament       tournament = TestFixtures.makeTournament(16, 4, 32, 8, 4, io.github.redouanebali.model.format.DrawMode.SEEDED);
    List<PlayerPair> teams      = TestFixtures.createPlayerPairs(16);
    TournamentBuilder.setupAndPopulateTournament(tournament, teams);

    Round q1Round  = tournament.getRoundByStage(Stage.Q1);
    Round q2Round  = tournament.getRoundByStage(Stage.Q2);
    Round r32Round = tournament.getRoundByStage(Stage.R32);

    // Simuler Q1 et propager vers Q2
    for (Game game : q1Round.getGames()) {
      if (game.getTeamA() != null && !game.getTeamA().isBye()) {
        game.setWinnerSide(TeamSide.TEAM_A);
      }
    }
    TournamentBuilder.propagateWinners(tournament);

    // Créer 4 gagnants identifiables pour Q2
    List<PlayerPair> q2Winners = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      PlayerPair winner = new PlayerPair();
      winner.setPlayer1(new Player("Match" + (i + 1)));
      winner.setPlayer2(new Player("Winner" + (i + 1)));
      winner.setSeed(200 + i);
      q2Winners.add(winner);
    }

    // Scénario : Les matchs se terminent dans le DÉSORDRE
    // Match 4 termine en premier, puis Match 2, puis Match 1, puis Match 3
    int[] executionOrder = {3, 1, 0, 2}; // Index des matchs qui se terminent dans cet ordre

    for (int execIdx = 0; execIdx < executionOrder.length; execIdx++) {
      int        matchIdx = executionOrder[execIdx];
      Game       game     = q2Round.getGames().get(matchIdx);
      PlayerPair winner   = q2Winners.get(matchIdx);
      game.setTeamA(winner);
      game.setTeamB(PlayerPair.bye());
      game.setWinnerSide(TeamSide.TEAM_A);

      // Propager immédiatement après chaque match
      TournamentBuilder.propagateWinners(tournament);
    }

    // Then: Vérifier que les gagnants sont QUAND MÊME dans le bon ordre
    List<QualifierSlotInfo> qualifierSlots = new ArrayList<>();
    for (int gameIdx = 0; gameIdx < r32Round.getGames().size(); gameIdx++) {
      Game game = r32Round.getGames().get(gameIdx);

      if (game.getTeamA() != null && game.getTeamA().getSeed() >= 200 && game.getTeamA().getSeed() < 300) {
        qualifierSlots.add(new QualifierSlotInfo(gameIdx, true, "TeamA"));
      }
      if (game.getTeamB() != null && game.getTeamB().getSeed() >= 200 && game.getTeamB().getSeed() < 300) {
        qualifierSlots.add(new QualifierSlotInfo(gameIdx, false, "TeamB"));
      }
    }

    for (int i = 0; i < qualifierSlots.size(); i++) {
      QualifierSlotInfo slot       = qualifierSlots.get(i);
      Game              r32Game    = r32Round.getGames().get(slot.gameIndex);
      PlayerPair        actualTeam = slot.isTeamA ? r32Game.getTeamA() : r32Game.getTeamB();

      int expectedMatchIndex = i; // Le slot Q1 doit contenir le gagnant du match Q2-1, etc.
      int actualMatchIndex   = actualTeam.getSeed() - 200;

      assertEquals(expectedMatchIndex, actualMatchIndex,
                   String.format("Le slot Q%d doit contenir le gagnant du match Q2-%d, mais contient le gagnant du match Q2-%d",
                                 i + 1, expectedMatchIndex + 1, actualMatchIndex + 1));
    }
  }

  @Test
  void testWinnerModificationAndCancellation() {
    // Given: Un tournoi simple avec R32 et R16
    Tournament tournament = TestFixtures.makeTournament(0, 0, 32, 0, 0, DrawMode.SEEDED);

    List<PlayerPair> teams = TestFixtures.createPlayerPairs(32);
    TournamentBuilder.setupAndPopulateTournament(tournament, teams);

    Round r32Round = tournament.getRoundByStage(Stage.R32);
    Round r16Round = tournament.getRoundByStage(Stage.R16);

    assertNotNull(r32Round);
    assertNotNull(r16Round);

    // Prendre le premier match de R32
    Game       game  = r32Round.getGames().get(0);
    PlayerPair teamA = game.getTeamA();
    PlayerPair teamB = game.getTeamB();

    assertNotNull(teamA);
    assertNotNull(teamB);

    // Définir le vainqueur comme teamA
    game.setScore(TestFixtures.createScoreWithWinner(game, teamA));
    TournamentBuilder.propagateWinners(tournament);

    // Vérifier que le vainqueur est propagé vers R16
    Game       nextGame   = r16Round.getGames().get(0);
    PlayerPair propagated = nextGame.getTeamA();
    assertEquals(teamA, propagated, "Le vainqueur teamA doit être propagé vers R16");

    // Modifier le vainqueur pour désigner teamB
    game.setScore(TestFixtures.createScoreWithWinner(game, teamB));
    TournamentBuilder.propagateWinners(tournament);

    // Vérifier que maintenant c'est teamB
    propagated = nextGame.getTeamA();
    assertEquals(teamB, propagated, "Le vainqueur modifié doit être teamB en R16");

    // Annuler la victoire
    game.setScore(null);
    TournamentBuilder.propagateWinners(tournament);

    // Vérifier que le match suivant est réinitialisé
    propagated = nextGame.getTeamA();
    assertNull(propagated, "Après annulation, le slot en R16 doit être réinitialisé");
  }

  /**
   * Classe helper pour stocker l'info d'un slot QUALIFIER
   */
  private record QualifierSlotInfo(int gameIndex, boolean isTeamA, String side) {

  }
}
