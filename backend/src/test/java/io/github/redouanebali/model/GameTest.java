package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

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

  @ParameterizedTest
  @CsvSource({
      // Format : nbSetsToWin, ptsPerSet, scoreString, superTieBreakFinalSet, expected

      // --- 1 set gagnant, 6 jeux par set ---
      "1, 6, '6-3', false, true",
      "1, 6, '7-5', false, true",
      "1, 6, '7-6', false, true",
      "1, 6, '6-5', false, false",
      "1, 6, '5-7', false, true",
      "1, 6, '6-7', false, true",
      "1, 6, '5-5', false, false",
      // --- 1 set gagnant, 9 jeux par set ---
      "1, 9, '9-7', false, true",
      "1, 9, '8-6', false, false",
      // --- 1 set gagnant, 4 jeux par set (tie-break à 4-4) ---
      "1, 4, '4-2', false, true",
      "1, 4, '5-4', false, true",
      "1, 4, '3-4', false, false",
      "1, 4, '3-3', false, false",
      // --- 2 sets gagnants, 6 jeux par set ---
      "2, 6, '6-0,6-0', false, true",
      "2, 6, '6-4,6-3', false, true",
      "2, 6, '6-3,4-6', false, false",
      "2, 6, '6-4,3-6,6-2', false, true",
      "2, 6, '6-4,3-6,5-5', false, false",
      "2, 6, '6-4,4-6,6-5', false, false",
      "2, 6, '6-4,4-6,7-5', false, true",
      "2, 6, '7-6,6-4', false, true",
      "2, 6, '7-6,6-5', false, false",
      // --- 2 sets gagnants, 4 jeux par set ---
      "2, 4, '4-2,4-2', false, true",
      "2, 4, '4-2,2-4', false, false",
      "2, 4, '4-2,2-4,4-3', false, false",
      "2, 4, '4-2,2-4,3-3', false, false",
      "2, 4, '4-2,2-4,5-4', false, true",
      // --- Super tie-break en 3e set ---
      "2, 6, '6-4,4-6,10-8', true, true",
      "2, 6, '6-4,4-6,10-9', true, false",
      "2, 6, '6-4,4-6,8-10', true, true",
      "2, 6, '6-4,4-6,9-11', true, true",
      "2, 6, '6-4,4-6,11-10', true, false",
      "2, 6, '6-4,4-6,11-13', true, true",
      "2, 6, '6-4,4-6,11-9', true, true",
      "2, 6, '6-4,4-6,8-8', true, false"
  })
  void testIsGameFinished_withFormat(
      int numberOfSetsToWin,
      int pointsPerSet,
      String scoreString,
      boolean isSuperTieBreakInFinalSet,
      boolean expected
  ) {
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(numberOfSetsToWin);
    format.setGamesPerSet(pointsPerSet);
    format.setSuperTieBreakInFinalSet(isSuperTieBreakInFinalSet);

    Game game = new Game(createSimpleFormat());
    game.setFormat(format);
    game.setTeamA(new PlayerPair());
    game.setTeamB(new PlayerPair());

    Score score = new Score();
    for (String set : scoreString.split(",")) {
      String[] parts = set.trim().split("-");
      score.addSetScore(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
    game.setScore(score);

    boolean result = game.isFinished();
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @CsvSource({
      // Format : nbSetsToWin, ptsPerSet, scoreString, superTieBreakFinalSet, expectedWinner

      // Matchs terminés et non terminés, divers formats

      // 2 sets gagnants, 6 jeux par set, sans super tie-break
      "2, 6, '6-3,6-1', false, A",
      "2, 6, '4-6,3-6', false, B",
      "2, 6, '6-4,3-6,6-2', false, A",
      "2, 6, '6-3,5-5', false, NONE",

      // 1 set gagnant, 6 jeux par set
      "1, 6, '6-3', false, A",
      "1, 6, '5-7', false, B",

      // 1 set gagnant, 9 jeux par set
      "1, 9, '9-7', false, A",

      // 1 set gagnant, 4 jeux par set
      "1, 4, '3-4', false, NONE",
      "1, 4, '4-2', false, A",

      // 2 sets gagnants, 4 jeux par set
      "2, 4, '4-2,2-4', false, NONE",
      "2, 4, '4-2,2-4,4-3', false, NONE",

      // Super tie-break activé - fin au 3e set, 10 points gagnants
      "2, 6, '6-4,4-6,10-8', true, A",
      "2, 6, '6-4,4-6,10-9', true, NONE",
      "2, 6, '6-4,4-6,8-10', true, B",
      "2, 6, '6-4,4-6,9-11', true, B",
      "2, 6, '6-4,4-6,11-10', true, NONE",
      "2, 6, '6-4,4-6,11-13', true, B",
      "2, 6, '6-4,4-6,11-9', true, A",
      "2, 6, '6-4,4-6,8-8', true, NONE"
  })
  void testGetWinner_withFormat(
      int numberOfSetsToWin,
      int pointsPerSet,
      String scoreString,
      boolean isSuperTieBreakInFinalSet,
      String expectedWinner
  ) {
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(numberOfSetsToWin);
    format.setGamesPerSet(pointsPerSet);
    format.setSuperTieBreakInFinalSet(isSuperTieBreakInFinalSet);

    PlayerPair teamA = new PlayerPair();
    teamA.setId(1L);
    PlayerPair teamB = new PlayerPair();
    teamB.setId(2L);

    Game game = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    game.setFormat(format);

    Score score = new Score();
    for (String set : scoreString.split(",")) {
      String[] parts = set.trim().split("-");
      score.addSetScore(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
    game.setScore(score);

    PlayerPair actual = game.getWinner();

    switch (expectedWinner) {
      case "A" -> assertEquals(teamA, actual);
      case "B" -> assertEquals(teamB, actual);
      case "NONE" -> assertNull(actual);
      default -> fail("Invalid expectedWinner value: " + expectedWinner);
    }
  }

  private MatchFormat createSimpleFormat() {
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(1);
    format.setGamesPerSet(6);
    format.setSuperTieBreakInFinalSet(false);
    return format;
  }

}
