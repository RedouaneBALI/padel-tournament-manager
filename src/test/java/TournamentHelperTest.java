import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.TournamentHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TournamentHelperTest {

  static Stream<org.junit.jupiter.params.provider.Arguments> provideFirstRoundCases() {
    return Stream.of(
        org.junit.jupiter.params.provider.Arguments.of(13, 16, 3),
        org.junit.jupiter.params.provider.Arguments.of(8, 8, 0),
        org.junit.jupiter.params.provider.Arguments.of(17, 32, 15),
        org.junit.jupiter.params.provider.Arguments.of(32, 32, 0)
    );
  }

  static Stream<org.junit.jupiter.params.provider.Arguments> provideSnakeGamesCases() {
    return Stream.of(
        // teamCount, expectedMatchCount, expectedByeVsByeCount
        org.junit.jupiter.params.provider.Arguments.of(8, 4, 0),
        org.junit.jupiter.params.provider.Arguments.of(6, 3, 0), // 6 teams + 2 BYES = 8, 4 matchs possibles, mais 1 BYE vs BYE non créé
        org.junit.jupiter.params.provider.Arguments.of(7, 4, 0), // 7 teams + 1 BYE = 8
        org.junit.jupiter.params.provider.Arguments.of(13, 7, 0) // 13 teams + 3 BYES = 16, 8 matchs, aucun BYE vs BYE
    );
  }

  // -------- Tests paramétrés pour generateFirstRoundWithByes --------
  @ParameterizedTest
  @MethodSource("provideFirstRoundCases")
  public void testGenerateFirstRoundWithByes_Param(int teamCount, int expectedDrawSize, int expectedByeCount) {
    List<PlayerPair> pairs = createPairs(teamCount);
    List<PlayerPair> round = TournamentHelper.generateFirstRoundWithByes(pairs);
    assertEquals(expectedDrawSize, round.size(), "Le premier tour doit contenir " + expectedDrawSize + " équipes");
    long byeCount = round.stream().filter(this::isBye).count();
    assertEquals(expectedByeCount, byeCount, "Il doit y avoir " + expectedByeCount + " BYES");
  }

  // -------- Test paramétré pour generateFirstRoundGamesSnake --------
  @ParameterizedTest
  @MethodSource("provideSnakeGamesCases")
  public void testGenerateFirstRoundGamesSnake_Param(int teamCount, int expectedMatchCount, int expectedByeVsByeCount) {
    List<PlayerPair> pairs         = createPairs(teamCount);
    List<PlayerPair> pairsWithByes = TournamentHelper.generateFirstRoundWithByes(pairs);
    List<Game>       games         = TournamentHelper.generateFirstRoundGamesSnake(pairsWithByes);

    long byeVsByeGames = games.stream()
                              .filter(g -> isBye(g.getTeamA()) && isBye(g.getTeamB()))
                              .count();
    assertEquals(expectedByeVsByeCount, byeVsByeGames, "Il doit y avoir " + expectedByeVsByeCount + " match(s) BYE vs BYE");
    assertEquals(expectedMatchCount, games.size(), "Il doit y avoir " + expectedMatchCount + " match(s) réel(s)");
  }

  private boolean isBye(PlayerPair pair) {
    return pair.getPlayer1().getId() == -1L && pair.getPlayer2().getId() == -1L;
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

  // Reste le test "snake" pour placement précis, qui peut rester en @Test classique :
  @Test
  public void testGenerateFirstRoundGamesSnake_8Teams_SnakeOrder() {
    List<PlayerPair> pairs = createPairs(8);
    List<Game>       games = TournamentHelper.generateFirstRoundGamesSnake(pairs);

    List<PlayerPair> snakeOrdered = TournamentHelper.snakeOrder(pairs);
    assertEquals(snakeOrdered.get(0), games.get(0).getTeamA());
    assertEquals(snakeOrdered.get(7), games.get(0).getTeamB());
    assertEquals(snakeOrdered.get(1), games.get(1).getTeamA());
    assertEquals(snakeOrdered.get(6), games.get(1).getTeamB());
    assertEquals(snakeOrdered.get(2), games.get(2).getTeamA());
    assertEquals(snakeOrdered.get(5), games.get(2).getTeamB());
    assertEquals(snakeOrdered.get(3), games.get(3).getTeamA());
    assertEquals(snakeOrdered.get(4), games.get(3).getTeamB());
  }
}