import static io.github.redouanebali.model.GameHelper.isMatchOver;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Score;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ScoreTest {

  @Test
  public void testAddSetScore() {
    Score score = new Score();
    score.addSetScore(6, 4);
    score.addSetScore(7, 5);

    assertEquals(2, score.getSets().size());
    assertEquals(6, score.getSets().getFirst().getTeamAScore());
    assertEquals(4, score.getSets().getFirst().getTeamBScore());
  }

  @Test
  public void testCompleteScoreRecording() {
    Score score = new Score();
    score.addSetScore(6, 4);
    score.addSetScore(4, 6);
    score.addSetScore(10, 7); // Assuming this is super tie-break but recorded as a set

    assertEquals(3, score.getSets().size());
    assertEquals(10, score.getSets().get(2).getTeamAScore());
    assertEquals(7, score.getSets().get(2).getTeamBScore());
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
    Game       game  = new Game();
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
    MatchFormat format = new MatchFormat(numberOfSetsToWin, pointsPerSet, superTieBreakInFinalSet);
    assertEquals(expectedComplete, isMatchOver(score, format));
  }

}
