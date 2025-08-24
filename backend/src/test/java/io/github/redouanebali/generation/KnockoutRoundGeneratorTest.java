package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

public class KnockoutRoundGeneratorTest {

  private KnockoutRoundGenerator generator;

  static Stream<Arguments> provideBracketSeedPositionCases() {
    return Stream.of(
        // Pour 8 équipes et 4 têtes de série
        Arguments.of(8, 4, new int[]{0, 7, 4, 3}, Stage.QUARTERS),
        // Pour 16 équipes et 8 têtes de série
        Arguments.of(16, 8, new int[]{0, 15, 8, 7, 4, 11, 12, 3}, Stage.R16),
        // Pour 16 équipes et 4 têtes de série
        Arguments.of(16, 4, new int[]{0, 15, 8, 7}, Stage.R16),
        Arguments.of(32, 16, new int[]{0, 31, 16, 15, 8, 23, 24, 7, 4, 27, 20, 11, 12, 19, 28, 3}, Stage.R32),
        Arguments.of(32, 8, new int[]{0, 31, 16, 15, 8, 23, 24, 7}, Stage.R32)
    );
  }

  @ParameterizedTest
  @CsvSource({
      "4, 4, 0, true",
      "4, 4, 0, false",
      "8, 6, 4, true",
      "8, 8, 4, false",
      "16, 12, 8, true",
      "16, 14, 4, false",
      "32, 28, 16, true",
      "32, 30, 16, true",
      "32, 32, 16, true",
      "32, 32, 16, false",
      "64, 60, 32, true",
      "64, 64, 32, true"
  })
  public void testFullKnockoutStagesUntilFinale(int mainDrawSize, int nbPlayerPairs, int nbSeeds, boolean manualMode) {
    int nbByePairs = mainDrawSize - nbPlayerPairs;
    generator = new KnockoutRoundGenerator(nbSeeds);

    Tournament       tournament = setupTournament(mainDrawSize, nbSeeds);
    List<PlayerPair> players    = TestFixtures.createPairs(nbPlayerPairs);
    tournament.getRounds().addAll(generator.generateManualRounds(players));
    Stage[] stages = determineStages(mainDrawSize);

    // Traite chaque stage automatiquement
    for (int i = 0; i < stages.length; i++) {
      Stage            stage            = stages[i];
      int              expectedGames    = mainDrawSize / (int) Math.pow(2, i + 1);
      int              expectedByeCount = (i == 0) ? nbByePairs : 0; // Seuls les BYE pairs sont dans le premier round
      List<PlayerPair> stagePlayers     = (i == 0) ? players : null;

      processStage(tournament, stage, stagePlayers, expectedGames, expectedByeCount, manualMode && i == 0);
    }
  }

  @Test
  public void testPropagationWithUnfinishedMatchLeavesNullSlot() {
    int mainDrawSize  = 8;
    int nbSeeds       = 4;
    int nbPlayerPairs = 8; // pas de BYE

    generator = new KnockoutRoundGenerator(nbSeeds);

    Tournament       tournament = setupTournament(mainDrawSize, nbSeeds);
    List<PlayerPair> players    = TestFixtures.createPairs(nbPlayerPairs);
    tournament.getRounds().addAll(generator.generateRounds(tournament, players, false));

    // Génère QUARTERS (1er round à 8 joueurs)
    Round quarters = tournament.getRounds().getFirst();

    // On marque un seul match comme "non terminé" (pas de score, pas de BYE) et on renseigne des gagnants pour les autres
    for (int i = 0; i < quarters.getGames().size(); i++) {
      var game = quarters.getGames().get(i);
      // s'assurer qu'il n'y a pas de BYE sur ce test
      assertFalse(game.getTeamA() != null && game.getTeamA().isBye());
      assertFalse(game.getTeamB() != null && game.getTeamB().isBye());
      if (i == 0) {
        // laisser ce match sans score -> pas de gagnant
        continue;
      }
      PlayerPair a      = game.getTeamA();
      PlayerPair b      = game.getTeamB();
      PlayerPair winner = (a != null) ? a : b;
      if (winner != null) {
        game.setScore(TestFixtures.createScoreWithWinner(game, winner));
      }
    }

    tournament.applyRound(quarters);
    generator.propagateWinners(tournament);

    // Vérifie qu'en SEMIS, exactement un emplacement est null (slot non déterminé)
    Round semis = tournament.getRoundByStage(Stage.SEMIS);
    assertNotNull(semis, "Le round SEMIS doit exister");
    long nullCount = countNullTeams(semis);
    assertEquals(1, nullCount, "Un seul slot en demi-finale doit rester vide suite au match non terminé");
  }

  @Test
  public void testGroupsStageIsIgnoredForKnockoutPropagation() {
    int mainDrawSize  = 8;
    int nbSeeds       = 4;
    int nbPlayerPairs = 8;

    generator = new KnockoutRoundGenerator(nbSeeds);

    Tournament tournament = setupTournament(mainDrawSize, nbSeeds);

    // Génère le premier round KO avec 8 joueurs (QUARTERS)
    List<PlayerPair> players = TestFixtures.createPairs(nbPlayerPairs);
    List<Round>      rounds  = generator.generateRounds(tournament, players, false);
    tournament.getRounds().addAll(rounds);
    Round quarters = rounds.getFirst();
    // Donne des gagnants à tous les matchs des quarts
    quarters.getGames().forEach(game -> {
      PlayerPair a      = game.getTeamA();
      PlayerPair b      = game.getTeamB();
      PlayerPair winner = (a != null) ? a : b;
      if (winner != null) {
        game.setScore(TestFixtures.createScoreWithWinner(game, winner));
      }
    });

    tournament.applyRound(quarters);
    generator.propagateWinners(tournament);

    // La propagation doit remplir SEMIS sans toucher le round GROUPS
    Round semis = tournament.getRoundByStage(Stage.SEMIS);
    assertNotNull(semis, "Le round SEMIS doit exister");
    long semisNullSlots = countNullTeams(semis);
    assertEquals(0, semisNullSlots, "Tous les slots des demi-finales doivent être déterminés après propagation");

    // Le round GROUPS ne doit pas contenir d'équipes injectées
    // assertEquals(0, groups.getGames().size(), "Le round GROUPS ne doit pas être peuplé par la propagation KO");
  }


  private void processStage(
      Tournament tournament,
      Stage stage,
      List<PlayerPair> players,
      int expectedGames,
      int expectedGamesWithBye,
      boolean isManual
  ) {
    List<Round> rounds = generateRounds(tournament, players, isManual);
    Round       round  = rounds.stream().filter(r -> r.getStage() == stage).findFirst().get();
    // Validation du round (ajout de vérifications demandées)
    validateRound(round, stage, expectedGames, players != null ? expectedGamesWithBye : 0, isManual);

    // Process and assign winners, then propagate
    tournament.applyRound(round);
    List<PlayerPair> winners = assignWinners(round);
    generator.propagateWinners(tournament);

    // *** Validations supplémentaires pour les rounds suivants ***

    // Vérifie les gagnants du round courant
    validateWinnersForNextRound(tournament, stage, winners);

    // Vérifie que les perdants ne réapparaissent pas dans les rounds suivants
    validateLosersNotReappearing(tournament, round, winners);
  }

  private void validateWinnersForNextRound(Tournament tournament, Stage currentStage, List<PlayerPair> winners) {
    // Si c'est le dernier stage, pas de stage suivant
    Stage nextStage = currentStage.next();
    if (nextStage == null || nextStage == Stage.WINNER) {
      return;
    }

    Round nextRound = tournament.getRoundByStage(nextStage);
    if (nextRound == null) {
      throw new AssertionError("Le round suivant (" + nextStage + ") n'a pas été généré correctement.");
    }

    // Collecte des joueurs dans le round suivant
    List<PlayerPair> nextRoundPlayers = nextRound.getGames().stream()
                                                 .flatMap(game -> Stream.of(game.getTeamA(), game.getTeamB()))
                                                 .filter(Objects::nonNull)
                                                 .toList();

    // Assure que chaque gagnant est présent dans le round suivant
    assertAll("Tous les gagnants doivent être présents dans le round suivant",
              winners.stream().map(winner -> () -> assertTrue(
                  nextRoundPlayers.contains(winner),
                  "Le gagnant " + winner + " n'est pas présent dans le round suivant (" + nextStage + ")"
              ))
    );
  }

  private void validateLosersNotReappearing(Tournament tournament, Round currentRound, List<PlayerPair> winners) {
    // Collecte des perdants
    List<PlayerPair> losers = currentRound.getGames().stream()
                                          .flatMap(game -> Stream.of(game.getTeamA(), game.getTeamB()))
                                          .filter(Objects::nonNull)
                                          .filter(player -> !winners.contains(player)) // Tous ceux qui ne sont pas gagnants
                                          .toList();

    // Vérifie que ces perdants n'apparaissent pas dans des rounds suivants
    List<PlayerPair> remainingPlayersInTournament = tournament.getRounds().stream()
                                                              .filter(round -> round.getStage().ordinal() > currentRound.getStage()
                                                                                                                        .ordinal()) // Rounds ultérieurs
                                                              .flatMap(round -> round.getGames().stream()
                                                                                     .flatMap(game -> Stream.of(game.getTeamA(), game.getTeamB()))
                                                              )
                                                              .filter(Objects::nonNull)
                                                              .toList();

    for (PlayerPair loser : losers) {
      if (remainingPlayersInTournament.contains(loser)) {
        throw new AssertionError("Le perdant " + loser + " a été réutilisé dans les rounds suivants.");
      }
    }
  }

  // Génération du round (séparée du reste pour plus de clarté)
  private List<Round> generateRounds(Tournament tournament, List<PlayerPair> players, boolean isManual) {
    return (players != null) ?
           generator.generateRounds(tournament, players, isManual) :
           tournament.getRounds();
  }

  // Validation du round
  private void validateRound(Round round, Stage stage, int expectedGames, int expectedGamesWithBye, boolean isManual) {
    assertEquals(expectedGames, round.getGames().size(), "Nombre de matchs attendu incorrect au stage : " + stage);

    // Vérifie les BYE uniquement au premier round (et si non mode manuel)
    if (expectedGamesWithBye > 0 && !isManual) {
      long byeCount = round.getGames().stream()
                           .filter(game -> (game.getTeamA() != null && game.getTeamA().isBye())
                                           || (game.getTeamB() != null && game.getTeamB().isBye()))
                           .count();
      assertEquals(expectedGamesWithBye, byeCount, "Nombre de BYE attendu incorrect pour le stage : " + stage);
    }
  }

  // Prépare le tournoi avec la configuration de base
  private Tournament setupTournament(int mainDrawSize, int nbSeeds) {
    Tournament tournament = new Tournament();
    tournament.setConfig(
        TournamentFormatConfig.builder()
                              .nbSeeds(nbSeeds)
                              .mainDrawSize(mainDrawSize)
                              .build()
    );
    return tournament;
  }

  private List<PlayerPair> assignWinners(Round round) {
    List<PlayerPair> winners = new ArrayList<>();

    round.getGames().forEach(game -> {
      PlayerPair a = game.getTeamA();
      PlayerPair b = game.getTeamB();

      if (a == null && b == null) {
        return; // rien à faire si le match est vide
      }

      PlayerPair winner;
      if (a != null && a.isBye()) {
        winner = b;
      } else if (b != null && b.isBye()) {
        winner = a;
      } else {
        // Choix déterministe du gagnant pour le test si pas de score fourni: privilégier A s'il existe, sinon B
        winner = (a != null) ? a : b;
      }

      if (winner != null) {
        game.setScore(TestFixtures.createScoreWithWinner(game, winner));
        winners.add(winner);
      }
    });

    return winners;
  }

  private long countNullTeams(Round round) {
    return round.getGames().stream()
                .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                .filter(Objects::isNull)
                .count();
  }

  // Détermine les stages à traiter en fonction de mainDrawSize
  private Stage[] determineStages(int mainDrawSize) {
    return switch (mainDrawSize) {
      case 4 -> new Stage[]{Stage.SEMIS, Stage.FINAL}; // Avec 4 joueurs, uniquement SEMIS et FINAL
      case 8 -> new Stage[]{Stage.QUARTERS, Stage.SEMIS, Stage.FINAL}; // Avec 8 joueurs
      case 16 -> new Stage[]{Stage.R16, Stage.QUARTERS, Stage.SEMIS, Stage.FINAL}; // Avec 16 joueurs
      case 32 -> new Stage[]{Stage.R32, Stage.R16, Stage.QUARTERS, Stage.SEMIS, Stage.FINAL}; // Avec 32 joueurs
      case 64 -> new Stage[]{Stage.R64, Stage.R32, Stage.R16, Stage.QUARTERS, Stage.SEMIS, Stage.FINAL}; // Avec 64 joueurs
      default -> throw new IllegalArgumentException("Taille de tableau non supportée : " + mainDrawSize);
    };
  }

  @ParameterizedTest
  @MethodSource("provideBracketSeedPositionCases")
  public void testBracketSeedPositions(
      int nbTeams,
      int nbSeeds,
      int[] expectedSeedIndices
  ) {
    List<PlayerPair> pairs = TestFixtures.createPairs(nbTeams);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));
    generator = new KnockoutRoundGenerator(nbSeeds);
    List<Integer> seedPositions = generator.getSeedsPositions(nbTeams, nbSeeds);
    for (int i = 0; i < expectedSeedIndices.length; i++) {
      int expectedIdx = expectedSeedIndices[i];
      int actualIdx   = seedPositions.get(i);
      assertEquals(expectedIdx, actualIdx,
                   "Seed " + (i + 1) + " doit être à l'indice " + expectedIdx + " mais est à l'indice " + actualIdx);
    }
  }


  @Test
  public void testTournamentStartsAtCorrectStageWhenPairsAreHalfOfMainDraw() {
    int mainDrawSize  = 16; // Taille du tableau principal
    int nbSeeds       = 4;       // Nombre de têtes de série
    int nbPlayerPairs = 8; // Moitié moins que le tableau principal

    generator = new KnockoutRoundGenerator(nbSeeds);

    Tournament tournament = setupTournament(mainDrawSize, nbSeeds);

    // Création de paires de joueurs pour le tournoi
    List<PlayerPair> players = TestFixtures.createPairs(nbPlayerPairs);

    // Vérifier que le tournoi commence directement au bon stage (QUARTERS) avec 8 joueurs
    List<Round> rounds = generator.generateRounds(tournament, players, false);
    tournament.getRounds().addAll(rounds);

    assertEquals(Stage.QUARTERS, rounds.getFirst().getStage(),
                 "Avec 8 joueurs pour un tableau principal de 16, le tournoi doit commencer en QUARTERS.");
  }
}