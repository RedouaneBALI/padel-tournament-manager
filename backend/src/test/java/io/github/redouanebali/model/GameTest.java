package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class GameTest {

  @ParameterizedTest
  @CsvSource({
      "'6-4,6-4', 2, 6, false, true",
      "'7-5,7-5', 2, 6, false, true",
      "'6-0,6-0', 2, 6, false, true",
      "'6-0,0-6,6-4', 2, 6, false, true",
      "'4-6,6-4,7-5', 2, 6, false, true",
      "'6-4,5-7', 2, 6, false, false",
      "'6-4,5-4', 2, 6, false, false",
      "'4-6,5-6', 2, 6, false, false",
      "'6-0,6-5', 2, 6, false, false",
      "'6-0,4-6,6-4', 2, 6, true, false",
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
  @CsvSource({
      "'6-4,6-4', 2, 6, false, A",
      "'4-6,4-6', 2, 6, false, B",
      "'6-4,4-6,7-5', 2, 6, false, A",
      "'4-6,6-4,5-7', 2, 6, false, B",
      "'6-4,4-6', 2, 6, false, null",
      "'6-4,4-6,10-7', 2, 6, true, A",
      "'6-4,4-6,7-10', 2, 6, true, B",
      "'9-7', 1, 9, false, A",
      "'4-9', 1, 9, false, B",
      "'5-3,5-4', 2, 4, false, A",
      "'3-5,4-5', 2, 4, false, B",
      "'4-5,5-4', 2, 4, false, null",
      "'6-0,4-6,6-4', 2, 6, true, null",
  })
  public void testGetWinner(String scores,
                            int numberOfSetsToWin,
                            int pointsPerSet,
                            boolean superTieBreakInFinalSet,
                            String expectedWinner) {

    PlayerPair teamA = new PlayerPair(-1L, new Player("A1"), new Player("A2"), 1);
    PlayerPair teamB = new PlayerPair(-1L, new Player("B1"), new Player("B2"), 2);
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
