package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("bali.redouane@gmail.com", null, List.of())
    );
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

  @ParameterizedTest
  @MethodSource("provideBracketSeedPositionCases")
  public void testGenerateGames(
      int nbTeams,
      int nbSeeds,
      int[] expectedSeedIndices,
      Stage expectedStage
  ) {
    List<PlayerPair> pairs = TestFixtures.createPairs(nbTeams);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));
    generator = new KnockoutRoundGenerator(nbSeeds);
    Round round = generator.generateAlgorithmicRound(pairs);
    assertEquals(expectedStage, round.getStage());
    List<Game> games = round.getGames();

    for (int i = 0; i < expectedSeedIndices.length; i++) {
      int        expectedPosition = expectedSeedIndices[i];
      int        gameIndex        = expectedPosition / 2;
      Game       game             = games.get(gameIndex);
      PlayerPair seedTeam         = pairs.get(i);

      assertEquals(seedTeam, game.getTeamA(),
                   String.format("seed %d should be in position %d (game %d)",
                                 seedTeam.getSeed(), expectedPosition, gameIndex));
    }

    Set<PlayerPair> allTeamsInGames = new HashSet<>();
    for (Game game : games) {
      if (game.getTeamA() != null) {
        allTeamsInGames.add(game.getTeamA());
      }
      if (game.getTeamB() != null) {
        allTeamsInGames.add(game.getTeamB());
      }
    }

    // Vérifier que toutes les équipes sont présentes
    assertEquals(nbTeams, allTeamsInGames.size(),
                 "Toutes les équipes doivent jouer dans un match");

    for (PlayerPair pair : pairs) {
      assertTrue(allTeamsInGames.contains(pair),
                 String.format("L'équipe avec seed %d doit jouer dans un match", pair.getSeed()));
    }

    Set<PlayerPair> duplicateCheck = new HashSet<>();
    for (Game game : games) {
      if (game.getTeamA() != null) {
        assertFalse(duplicateCheck.contains(game.getTeamA()),
                    String.format("L'équipe avec seed %d joue dans plusieurs matchs", game.getTeamA().getSeed()));
        duplicateCheck.add(game.getTeamA());
      }
      if (game.getTeamB() != null) {
        assertFalse(duplicateCheck.contains(game.getTeamB()),
                    String.format("L'équipe avec seed %d joue dans plusieurs matchs", game.getTeamB().getSeed()));
        duplicateCheck.add(game.getTeamB());
      }
    }

    assertEquals(nbTeams / 2, games.size(),
                 "Le nombre de matchs doit être égal à nbTeams / 2");
  }

  @Test
  void testGenerateManualRound() {

    PlayerPair pairA = new PlayerPair(1L, new Player("A"), new Player("B"), 1);
    PlayerPair pairB = new PlayerPair(2L, new Player("C"), new Player("D"), 2);
    PlayerPair pairC = new PlayerPair(3L, new Player("E"), new Player("F"), 3);
    PlayerPair pairD = new PlayerPair(4L, new Player("G"), new Player("H"), 4);

    List<PlayerPair> pairs = List.of(pairA, pairB, pairC, pairD);

    generator = new KnockoutRoundGenerator(0);
    Round      round = generator.generateManualRound(pairs);
    List<Game> games = round.getGames();
    assertEquals(2, games.size());

    Game game1 = games.get(0);
    assertEquals(pairA, game1.getTeamA());
    assertEquals(pairB, game1.getTeamB());

    Game game2 = games.get(1);
    assertEquals(pairC, game2.getTeamA());
    assertEquals(pairD, game2.getTeamB());
  }

  @Test
  void testPropagateWinners_UpdatesFinalWhenScoresChange() {
    // Pairs A,B,C,D with deterministic ids for createScoreWithWinner
    PlayerPair pairA = new PlayerPair(1L, new Player("A"), new Player("B"), 1);
    PlayerPair pairB = new PlayerPair(2L, new Player("C"), new Player("D"), 2);
    PlayerPair pairC = new PlayerPair(3L, new Player("E"), new Player("F"), 3);
    PlayerPair pairD = new PlayerPair(4L, new Player("G"), new Player("H"), 4);

    List<PlayerPair> pairs = List.of(pairA, pairB, pairC, pairD);

    // Generate manual round: A vs B, C vs D
    generator = new KnockoutRoundGenerator(0);
    Round semiFinals = generator.generateManualRound(pairs);

    // Ensure we have two games
    List<Game> semiGames = semiFinals.getGames();
    assertEquals(2, semiGames.size());
    MatchFormat matchFormat = TestFixtures.createSimpleFormat(1);
    // Set formats and initial winners: A beats B, C beats D
    Game semi0 = semiGames.get(0);
    semi0.setFormat(matchFormat);
    semi0.setScore(TestFixtures.createScoreWithWinner(semi0, pairA));

    Game semi1 = semiGames.get(1);
    semi1.setFormat(matchFormat);
    semi1.setScore(TestFixtures.createScoreWithWinner(semi1, pairC));

    // Prepare final round with one game
    Round finals = new Round();
    finals.setStage(Stage.FINAL);
    Game finalGame = new Game(matchFormat);
    finals.addGames(List.of(finalGame));

    // Tournament with both rounds in order
    Tournament tournament = new Tournament();
    tournament.setFormat(TournamentFormat.KNOCKOUT);
    List<Round> rounds = new LinkedList<>();
    rounds.add(semiFinals);
    rounds.add(finals);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(rounds);

    // First propagation: finalists should be A vs C
    generator.propagateWinners(tournament);
    assertEquals(pairA, finalGame.getTeamA());
    assertEquals(pairC, finalGame.getTeamB());

    // Change winners: set B and D as winners of their semis
    semi0.setScore(TestFixtures.createScoreWithWinner(semi0, pairB));
    semi1.setScore(TestFixtures.createScoreWithWinner(semi1, pairD));

    // Re-propagate and verify final updates to B vs D
    generator.propagateWinners(tournament);
    assertEquals(pairB, finalGame.getTeamA());
    assertEquals(pairD, finalGame.getTeamB());
  }

  @Test
  void testPropagateWinners() {
    Round roundCurrent = new Round();
    roundCurrent.setStage(Stage.R32);

    MatchFormat matchFormat = TestFixtures.createSimpleFormat(1);

    List<Game> gamesCurrent = new
        ArrayList<>();

    PlayerPair byePair = PlayerPair.bye();
    byePair.setId(99L);

    List<PlayerPair> pairs = TestFixtures.createPairs(6);

    Game game0 = new Game(matchFormat);
    game0.setTeamA(pairs.get(0));
    game0.setTeamB(pairs.get(1));
    game0.setScore(TestFixtures.createScoreWithWinner(game0, pairs.get(0)));

    Game game1 = new Game(matchFormat);
    game1.setTeamA(pairs.get(2));
    game1.setTeamB(byePair);
    game1.setScore(null);
    Game game2 = new Game(matchFormat);
    game2.setTeamA(pairs.get(3));
    game2.setTeamB(pairs.get(4));
    game2.setScore(null);

    // Match 3 : pair6 vs BYE (pair6 passe automatiquement)
    Game game3 = new Game(matchFormat);
    game3.setTeamA(pairs.get(5));
    game3.setTeamB(byePair);
    game3.setScore(null);

    gamesCurrent.add(game0);
    gamesCurrent.add(game1);
    gamesCurrent.add(game2);
    gamesCurrent.add(game3);

    roundCurrent.addGames(gamesCurrent);

    // Round suivant (ex: R16) avec 2 matchs (indices 0 et 1)
    Round roundNext = new Round();
    roundNext.setStage(Stage.R16);
    List<Game> gamesNext = new ArrayList<>();

    Game nextGame0 = new Game(matchFormat); // Correspond aux matchs 0 et 1 du round actuel
    Game nextGame1 = new Game(matchFormat); // Correspond aux matchs 2 et 3 du round actuel

    gamesNext.add(nextGame0);
    gamesNext.add(nextGame1);

    roundNext.addGames(gamesNext);

    // Tournoi
    Tournament tournament = new Tournament();
    tournament.setFormat(TournamentFormat.KNOCKOUT);
    List<Round> rounds = new LinkedList<>();
    rounds.add(roundCurrent);
    rounds.add(roundNext);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(rounds);

    // Action
    generator = new KnockoutRoundGenerator(0);
    generator.propagateWinners(tournament);

    // Vérification match nextGame0 (issu des matchs 0 et 1)
    // i=0 -> pair1 (vainqueur de game0) en teamA
    assertEquals(pairs.get(0), nextGame0.getTeamA());
    // i=1 -> pair3 (match gagné par BYE) en teamB
    assertEquals(pairs.get(2), nextGame0.getTeamB());

    // Vérification match nextGame1 (issu des matchs 2 et 3)
    // i=2 -> game2 non terminé donc teamA = null
    assertNull(nextGame1.getTeamA());
    // i=3 -> pair6 (BYE) en teamB
    assertEquals(pairs.get(5), nextGame1.getTeamB());
  }

}