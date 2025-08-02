import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Score;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class GameTest {

  private static Stream<Arguments> winnerTestCases() {
    return Stream.of(
        Arguments.of("6-4,6-4", 2, 6, false, "A"),
        Arguments.of("4-6,4-6", 2, 6, false, "B"),
        Arguments.of("6-4,4-6,7-5", 2, 6, false, "A"),
        Arguments.of("4-6,6-4,5-7", 2, 6, false, "B"),
        Arguments.of("6-4,4-6", 2, 6, false, "null") // match not finished
    );
  }

  @ParameterizedTest
  @CsvSource({
      "'6-4,6-4', 2, 6, false, true",
      "'7-5,7-5', 2, 6, false, true",
      "'6-0,6-0', 2, 6, false, true",
      "'4-6,6-4,7-5', 2, 6, false, true",
      "'6-4,5-7', 2, 6, false, false",
      "'6-4,5-4', 2, 6, false, false",
      "'4-6,5-6', 2, 6, false, false",
      "'6-0,6-5', 2, 6, false, false",
      "'9-7', 1, 9, false, true",
      "'5-3,5-4', 2, 4, false, true",
      "'5-3,3-5,5-3', 2, 4, false, true",
      "'5-3,4-3', 2, 4, false, false",
      "'6-4,4-6,10-7', 2, 6, true, true",
      "'6-4,4-6,9-11', 2, 6, true, true",
      "'6-4,4-6,7-10', 2, 6, true, true",
      "'6-4,4-6,7-8', 2, 6, true, false"
  })
  public void testGameCompletionParameterized(String scores,
                                              int numberOfSetsToWin,
                                              int pointsPerSet,
                                              boolean superTieBreakInFinalSet,
                                              boolean expectedComplete) {
    PlayerPair teamA = new PlayerPair(-1L, new Player(), new Player(), 1);
    PlayerPair teamB = new PlayerPair(-1L, new Player(), new Player(), 2);
    Game       game  = new Game(new MatchFormat(1L, numberOfSetsToWin, pointsPerSet, superTieBreakInFinalSet, false));
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score    score     = new Score();
    String[] setScores = scores.split(",");
    for (String set : setScores) {
      String[] parts      = set.split("-");
      int      teamAScore = Integer.parseInt(parts[0]);
      int      teamBScore = Integer.parseInt(parts[1]);
      score.addSetScore(teamAScore, teamBScore);
    }
    game.setScore(score);
    assertEquals(expectedComplete, game.isFinished());
  }

  @ParameterizedTest
  @MethodSource("winnerTestCases")
  public void testGetWinner(String scores,
                            int numberOfSetsToWin,
                            int pointsPerSet,
                            boolean superTieBreakInFinalSet,
                            String expectedWinner) {
    Player player1 = new Player();
    player1.setName("Player A1");
    Player player2 = new Player();
    player2.setName("Player A2");
    Player player3 = new Player();
    player3.setName("Player B1");
    Player player4 = new Player();
    player4.setName("Player B2");

    PlayerPair teamA = new PlayerPair(-1L, player1, player2, 1);
    PlayerPair teamB = new PlayerPair(-1L, player3, player4, 2);
    Game       game  = new Game(new MatchFormat(1L, numberOfSetsToWin, pointsPerSet, superTieBreakInFinalSet, false));
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score    score     = new Score();
    String[] setScores = scores.split(",");
    for (String set : setScores) {
      String[] parts      = set.split("-");
      int      teamAScore = Integer.parseInt(parts[0]);
      int      teamBScore = Integer.parseInt(parts[1]);
      score.addSetScore(teamAScore, teamBScore);
    }
    game.setScore(score);

    PlayerPair winner = game.getWinner();
    if (expectedWinner.equals("A")) {
      assertSame(teamA, winner);
    } else if (expectedWinner.equals("B")) {
      assertSame(teamB, winner);
    } else {
      assertNull(winner);
    }
  }

}
