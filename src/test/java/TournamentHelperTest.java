import static io.github.redouanebali.model.TournamentHelper.getSeedsPositions;
import static io.github.redouanebali.model.TournamentHelper.initGamesWithSeedTeams;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TournamentHelperTest {

  static Stream<Arguments> provideBracketSeedPositionCases() {
    return Stream.of(
        // Pour 8 équipes et 4 têtes de série
        Arguments.of(8, 4, new int[]{0, 7, 4, 3}),
        // Pour 16 équipes et 8 têtes de série
        Arguments.of(16, 8, new int[]{0, 15, 8, 7, 4, 11, 12, 3}),
        // Pour 16 équipes et 4 têtes de série
        Arguments.of(16, 4, new int[]{0, 15, 8, 7}),
        // Pour 32 équipes et 16 têtes de série
        Arguments.of(32, 16, new int[]{0, 31, 16, 15, 8, 23, 24, 7, 4, 27, 20, 11, 12, 19, 28, 3}),
        // Pour 32 équipes et 8 têtes de série
        Arguments.of(32, 8, new int[]{0, 31, 16, 15, 8, 23, 24, 7})
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
    List<Integer> seedPositions = getSeedsPositions(nbTeams, nbSeeds);
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
  public void testInitGamesWithSeedTeams(
      int nbTeams,
      int nbSeeds,
      int[] expectedSeedIndices
  ) {
    List<PlayerPair> pairs = createPairs(nbTeams);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));
    List<Game> games = initGamesWithSeedTeams(pairs, nbSeeds);
    for (int i = 0; i < expectedSeedIndices.length; i++) {
      int        expectedPosition = expectedSeedIndices[i];
      int        gameIndex        = expectedPosition / 2;
      Game       game             = games.get(gameIndex);
      PlayerPair seedTeam         = pairs.get(i);

      assertEquals(seedTeam, game.getTeamA(),
                   String.format("seed %d should be in position %d (game %d)",
                                 seedTeam.getSeed(), expectedPosition, gameIndex));
    }
  }

  private List<PlayerPair> createPairs(int count) {
    List<PlayerPair> pairs = new ArrayList<>();
    IntStream.rangeClosed(1, count).forEach(seed -> {
      Player player1 = new Player((long) seed, "Player" + seed + "A", seed, 0, 1990);
      Player player2 = new Player((long) seed + 100, "Player" + seed + "B", seed, 0, 1990);
      pairs.add(new PlayerPair(player1, player2, seed));
    });
    return pairs;
  }
}