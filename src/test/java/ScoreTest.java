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
  public void testSuperTieBreak() {
    Score score = new Score();
    score.setSuperTieBreakTeamA(10);
    score.setSuperTieBreakTeamB(8);

    assertEquals(10, score.getSuperTieBreakTeamA());
    assertEquals(8, score.getSuperTieBreakTeamB());
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
      "'6-4,6-4', true",
      "'7-5,7-5', true",
      "'6-0,6-0', true",
      "'4-6,6-4,7-5', true",
      "'6-4,5-7', false",
      "'6-4,5-4', false",
      "4-6,5-6', false"
  })
  public void testGameCompletionParameterized(String scores, boolean expectedComplete) {
    PlayerPair teamA = new PlayerPair(new Player(), new Player(), 1);
    PlayerPair teamB = new PlayerPair(new Player(), new Player(), 2);
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
    assertEquals(expectedComplete, game.isMatchOver(new MatchFormat(2, 6, false)));
  }

}
