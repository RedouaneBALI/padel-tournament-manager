package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
        // Pour 32 équipes et 16 têtes de série
        Arguments.of(32, 16, new int[]{0, 31, 16, 15, 8, 23, 24, 7, 4, 27, 20, 11, 12, 19, 28, 3}, Stage.R32),
        // Pour 32 équipes et 8 têtes de série
        Arguments.of(32, 8, new int[]{0, 31, 16, 15, 8, 23, 24, 7}, Stage.R32)
    );
  }

  @ParameterizedTest
  @MethodSource("provideBracketSeedPositionCases")
  public void testBracketSeedPositions(
      int nbTeams,
      int nbSeeds,
      int[] expectedSeedIndices
  ) {
    List<PlayerPair> pairs = createPairs(nbTeams);
    // On trie les équipes par seed croissant pour que seed 1 soit à l'indice 0, seed 2 à 1, etc.
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));
    generator = new KnockoutRoundGenerator(nbSeeds);
    List<Integer> seedPositions = generator.getSeedsPositions(nbTeams, nbSeeds);
    // Vérification pour tous les seeds concernés
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
    List<PlayerPair> pairs = createPairs(nbTeams);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));
    generator = new KnockoutRoundGenerator(nbSeeds);
    Round round = generator.generateAlgorithmicRound(pairs);
    assertEquals(expectedStage, round.getStage());
    List<Game> games = round.getGames();

    // Vérification du placement des têtes de série
    for (int i = 0; i < expectedSeedIndices.length; i++) {
      int        expectedPosition = expectedSeedIndices[i];
      int        gameIndex        = expectedPosition / 2;
      Game       game             = games.get(gameIndex);
      PlayerPair seedTeam         = pairs.get(i);

      assertEquals(seedTeam, game.getTeamA(),
                   String.format("seed %d should be in position %d (game %d)",
                                 seedTeam.getSeed(), expectedPosition, gameIndex));
    }

    // Vérification que toutes les équipes jouent dans un match
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

    // Vérification supplémentaire : aucune équipe ne joue deux fois
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
    Round round = generator.generateManualRound(pairs);
    // Vérifications
    List<Game> games = round.getGames();
    assertEquals(2, games.size());

    Game game1 = games.get(0);
    assertEquals(pairA, game1.getTeamA());
    assertEquals(pairB, game1.getTeamB());

    Game game2 = games.get(1);
    assertEquals(pairC, game2.getTeamA());
    assertEquals(pairD, game2.getTeamB());
  }

  private List<PlayerPair> createPairs(int count) {
    List<PlayerPair> pairs = new ArrayList<>();
    IntStream.rangeClosed(1, count).forEach(seed -> {
      Player player1 = new Player((long) seed, "Player" + seed + "A", seed, 0, 1990);
      Player player2 = new Player((long) seed + 100, "Player" + seed + "B", seed, 0, 1990);
      pairs.add(new PlayerPair(-1L, player1, player2, seed));
    });
    return pairs;
  }
}