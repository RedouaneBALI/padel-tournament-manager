package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GameTest {

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
  void testGameCompletionParameterized(String scores,
                                       int numberOfSetsToWin,
                                       int pointsPerSet,
                                       boolean superTieBreakInFinalSet,
                                       boolean expectedComplete) {
    PlayerPair  teamA  = new PlayerPair(new Player(), new Player(), 1);
    PlayerPair  teamB  = new PlayerPair(new Player(), new Player(), 2);
    MatchFormat format = new MatchFormat(1L, numberOfSetsToWin, pointsPerSet, superTieBreakInFinalSet, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score score = parseScoreString(scores, format);
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
  void testGetWinner(String scores,
                     int numberOfSetsToWin,
                     int pointsPerSet,
                     boolean superTieBreakInFinalSet,
                     String expectedWinner) {

    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, numberOfSetsToWin, pointsPerSet, superTieBreakInFinalSet, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score score = parseScoreString(scores, format);
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
      // Format: nbSetsToWin, ptsPerSet, scoreString, superTieBreakFinalSet, expected

      // --- 1 winning set, 6 games per set ---
      "1, 6, '6-3', false, true",
      "1, 6, '7-5', false, true",
      "1, 6, '7-6', false, true",
      "1, 6, '6-5', false, false",
      "1, 6, '5-7', false, true",
      "1, 6, '6-7', false, true",
      "1, 6, '5-5', false, false",
      // --- 1 winning set, 9 games per set ---
      "1, 9, '9-7', false, true",
      "1, 9, '8-6', false, false",
      // --- 1 winning set, 4 games per set (tie-break at 4-4) ---
      "1, 4, '4-2', false, true",
      "1, 4, '5-4', false, true",
      "1, 4, '3-4', false, false",
      "1, 4, '3-3', false, false",
      // --- 2 winning sets, 6 games per set ---
      "2, 6, '6-0,6-0', false, true",
      "2, 6, '6-4,6-3', false, true",
      "2, 6, '6-3,4-6', false, false",
      "2, 6, '6-4,3-6,6-2', false, true",
      "2, 6, '6-4,3-6,5-5', false, false",
      "2, 6, '6-4,4-6,6-5', false, false",
      "2, 6, '6-4,4-6,7-5', false, true",
      "2, 6, '7-6,6-4', false, true",
      "2, 6, '7-6,6-5', false, false",
      // --- 2 winning sets, 4 games per set ---
      "2, 4, '4-2,4-2', false, true",
      "2, 4, '4-2,2-4', false, false",
      "2, 4, '4-2,2-4,4-3', false, false",
      "2, 4, '4-2,2-4,3-3', false, false",
      "2, 4, '4-2,2-4,5-4', false, true",
      // --- Super tie-break in 3rd set ---
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

    Game game = new Game(format);
    game.setTeamA(new PlayerPair());
    game.setTeamB(new PlayerPair());

    Score score = parseScoreString(scoreString, format);
    game.setScore(score);

    boolean result = game.isFinished();
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @CsvSource({
      // Format: nbSetsToWin, ptsPerSet, scoreString, superTieBreakFinalSet, expectedWinner

      // Completed and ongoing matches, various formats

      // 2 winning sets, 6 games per set, without super tie-break
      "2, 6, '6-3,6-1', false, A",
      "2, 6, '4-6,3-6', false, B",
      "2, 6, '6-4,3-6,6-2', false, A",
      "2, 6, '6-3,5-5', false, NONE",

      // 1 winning set, 6 games per set
      "1, 6, '6-3', false, A",
      "1, 6, '5-7', false, B",

      // 1 winning set, 9 games per set
      "1, 9, '9-7', false, A",

      // 1 winning set, 4 games per set
      "1, 4, '3-4', false, NONE",
      "1, 4, '4-2', false, A",

      // 2 winning sets, 4 games per set
      "2, 4, '4-2,2-4', false, NONE",
      "2, 4, '4-2,2-4,4-3', false, NONE",

      // Super tie-break enabled - end in 3rd set, 10 winning points
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

    Score score = parseScoreString(scoreString, format);
    game.setScore(score);

    PlayerPair actual = game.getWinner();

    switch (expectedWinner) {
      case "A" -> assertEquals(teamA, actual);
      case "B" -> assertEquals(teamB, actual);
      case "NONE" -> assertNull(actual);
      default -> fail("Invalid expectedWinner value: " + expectedWinner);
    }
  }

  @ParameterizedTest
  @CsvSource({
      // Format: 1 set of 9 games, tie-break at 8-8
      // Score, isFinished, expectedWinner
      "'9-7', true, A",      // Classic victory (2 games difference before 8-8)
      "'9-8', true, A",      // Victory after tie-break at 8-8
      "'8-9', true, B",      // Victory after tie-break at 8-8
      "'9-6', true, A",      // Classic victory
      "'6-9', true, B",      // Classic victory
      "'8-8', false, NONE",  // Match in progress, tie-break upcoming
      "'8-7', false, NONE",  // Match in progress
      "'7-8', false, NONE",  // Match in progress
      "'8-6', false, NONE",  // Match in progress (not enough games to win)
      "'10-8', true, A",     // Victory after extension
      "'8-10', true, B",     // Victory after extension
  })
  void testFormat_1Set9Games_TieBreakAt8(String scoreString, boolean expectedFinished, String expectedWinner) {
    // Format: 1 set of 9 games, tie-break at 8-8
    MatchFormat format = new MatchFormat(1L, 1, 9, false, false, 8);

    PlayerPair teamA = new PlayerPair();
    teamA.setId(1L);
    PlayerPair teamB = new PlayerPair();
    teamB.setId(2L);

    Game game = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score score = new Score();
    for (String set : scoreString.split(",")) {
      String[] parts = set.trim().split("-");
      score.addSetScore(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
    game.setScore(score);

    // Test isFinished
    assertEquals(expectedFinished, game.isFinished(),
                 "isFinished should be " + expectedFinished + " for score " + scoreString);

    // Test getWinner
    PlayerPair actual = game.getWinner();
    switch (expectedWinner) {
      case "A" -> assertEquals(teamA, actual, "Winner should be team A for score " + scoreString);
      case "B" -> assertEquals(teamB, actual, "Winner should be team B for score " + scoreString);
      case "NONE" -> assertNull(actual, "Winner should be null for score " + scoreString);
      default -> fail("Invalid expectedWinner value: " + expectedWinner);
    }
  }

  @Test
  void testMatchFormat_TieBreakAt_Validation() {
    // Test that tieBreakAt must be gamesPerSet or gamesPerSet-1
    MatchFormat format = new MatchFormat();
    format.setGamesPerSet(9);

    // Valid values
    format.setTieBreakAt(9);  // OK
    format.setTieBreakAt(8);  // OK

    // Invalid value
    assertThrows(IllegalArgumentException.class, () -> format.setTieBreakAt(7));
    assertThrows(IllegalArgumentException.class, () -> format.setTieBreakAt(10));
  }

  private MatchFormat createSimpleFormat() {
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(1);
    format.setGamesPerSet(6);
    format.setSuperTieBreakInFinalSet(false);
    return format;
  }

  /**
   * Helper method to parse score string and create Score object with proper super tie-break handling. Eliminates code duplication across test
   * methods.
   *
   * @param scoreString format: "6-4,6-3" or "6-4 6-3"
   * @param format the match format (used to determine super tie-break handling)
   * @return populated Score object
   */
  private Score parseScoreString(String scoreString, MatchFormat format) {
    Score    score     = new Score();
    String[] setScores = scoreString.replaceAll("\\s+", ",").split(",");

    boolean isSuperTieBreakEnabled = format.isSuperTieBreakInFinalSet();
    int     expectedTotalSets      = 2 * format.getNumberOfSetsToWin() - 1;

    for (int i = 0; i < setScores.length; i++) {
      String   set        = setScores[i].trim();
      String[] parts      = set.split("-");
      int      teamAScore = Integer.parseInt(parts[0]);
      int      teamBScore = Integer.parseInt(parts[1]);

      SetScore setScore = new SetScore(teamAScore, teamBScore);

      // Handle super tie-break in final set
      boolean isFinalSet = (i == setScores.length - 1) && (setScores.length == expectedTotalSets);
      if (isSuperTieBreakEnabled && isFinalSet && (teamAScore >= 7 || teamBScore >= 7)) {
        setScore.setTieBreakTeamA(teamAScore);
        setScore.setTieBreakTeamB(teamBScore);
        setScore.setTeamAScore(teamAScore > teamBScore ? 1 : 0);
        setScore.setTeamBScore(teamBScore > teamAScore ? 1 : 0);
      }

      score.getSets().add(setScore);
    }
    return score;
  }

  // ============= FORFEIT TESTS =============

  @Test
  void testForfeit_teamAForfeits_teamBWins() {
    MatchFormat standardFormat = new MatchFormat(null, 2, 6, false, true);
    PlayerPair  teamA          = new PlayerPair("Alice", "Bob", 1);
    PlayerPair  teamB          = new PlayerPair("Charlie", "Dave", 2);

    Game game = new Game();
    game.setFormat(standardFormat);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score forfeitScore = Score.forfeit(TeamSide.TEAM_A);
    game.setScore(forfeitScore);

    assertTrue(game.isFinished(), "Game should be finished when forfeit");
    assertEquals(teamB, game.getWinner(), "Team B should win when Team A forfeits");
    assertEquals(TeamSide.TEAM_B, game.getWinnerSide(), "Winner side should be TEAM_B");
  }

  @Test
  void testForfeit_teamBForfeits_teamAWins() {
    MatchFormat standardFormat = new MatchFormat(null, 2, 6, false, true);
    PlayerPair  teamA          = new PlayerPair("Alice", "Bob", 1);
    PlayerPair  teamB          = new PlayerPair("Charlie", "Dave", 2);

    Game game = new Game();
    game.setFormat(standardFormat);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score forfeitScore = Score.forfeit(TeamSide.TEAM_B);
    game.setScore(forfeitScore);

    assertTrue(game.isFinished(), "Game should be finished when forfeit");
    assertEquals(teamA, game.getWinner(), "Team A should win when Team B forfeits");
    assertEquals(TeamSide.TEAM_A, game.getWinnerSide(), "Winner side should be TEAM_A");
  }

  @Test
  void testForfeit_noForfeitedBySpecified_noWinner() {
    MatchFormat standardFormat = new MatchFormat(null, 2, 6, false, true);
    PlayerPair  teamA          = new PlayerPair("Alice", "Bob", 1);
    PlayerPair  teamB          = new PlayerPair("Charlie", "Dave", 2);

    Game game = new Game();
    game.setFormat(standardFormat);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score score = new Score();
    score.setForfeit(true);
    game.setScore(score);

    assertTrue(game.isFinished(), "Game should be finished");
    assertNull(game.getWinner(), "No winner should be determined if forfeitedBy is not specified");
    assertNull(game.getWinnerSide(), "Winner side should be null");
  }

  @Test
  void testForfeit_withPartialScore_teamALeading() {
    MatchFormat standardFormat = new MatchFormat(null, 2, 6, false, true);
    PlayerPair  teamA          = new PlayerPair("Alice", "Bob", 1);
    PlayerPair  teamB          = new PlayerPair("Charlie", "Dave", 2);

    Game game = new Game();
    game.setFormat(standardFormat);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score partialScore = new Score();
    partialScore.addSetScore(6, 2);
    partialScore.addSetScore(2, 0);
    Score forfeitScore = Score.forfeitWithPartialScore(partialScore, TeamSide.TEAM_B);
    game.setScore(forfeitScore);

    assertTrue(game.isFinished(), "Game should be finished");
    assertEquals(teamA, game.getWinner(), "Team A should win when Team B forfeits");
    assertEquals(TeamSide.TEAM_A, game.getWinnerSide());
    assertEquals(2, game.getScore().getSets().size(), "Partial score should be preserved");
    assertEquals("6-2 2-0 (Forfeit by TEAM_B)", game.getScore().toString());
  }

  @Test
  void testForfeit_withPartialScore_evenScore() {
    MatchFormat standardFormat = new MatchFormat(null, 2, 6, false, true);
    PlayerPair  teamA          = new PlayerPair("Alice", "Bob", 1);
    PlayerPair  teamB          = new PlayerPair("Charlie", "Dave", 2);

    Game game = new Game();
    game.setFormat(standardFormat);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score partialScore = new Score();
    partialScore.addSetScore(6, 4);
    partialScore.addSetScore(3, 6);
    partialScore.addSetScore(2, 2);
    Score forfeitScore = Score.forfeitWithPartialScore(partialScore, TeamSide.TEAM_A);
    game.setScore(forfeitScore);

    assertTrue(game.isFinished(), "Game should be finished");
    assertEquals(teamB, game.getWinner(), "Team B should win when Team A forfeits, regardless of score");
    assertEquals(TeamSide.TEAM_B, game.getWinnerSide());
  }

  @Test
  void testMarkAsForfeit_duringMatch() {
    MatchFormat standardFormat = new MatchFormat(null, 2, 6, false, true);
    PlayerPair  teamA          = new PlayerPair("Alice", "Bob", 1);
    PlayerPair  teamB          = new PlayerPair("Charlie", "Dave", 2);

    Game game = new Game();
    game.setFormat(standardFormat);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score score = new Score();
    score.addSetScore(6, 4);
    score.addSetScore(2, 3);
    game.setScore(score);

    assertFalse(game.isFinished(), "Initially not finished");
    assertNull(game.getWinner());

    score.markAsForfeit(TeamSide.TEAM_B);
    game.setScore(score);

    assertTrue(game.isFinished(), "Should be finished after forfeit");
    assertEquals(teamA, game.getWinner(), "Team A should win after Team B forfeits");
    assertEquals(TeamSide.TEAM_A, game.getWinnerSide());
  }

  @Test
  void testBye_overridesForfeitLogic() {
    MatchFormat standardFormat = new MatchFormat(null, 2, 6, false, true);
    PlayerPair  teamA          = new PlayerPair("Alice", "Bob", 1);

    Game game = new Game();
    game.setFormat(standardFormat);
    game.setTeamA(teamA);
    game.setTeamB(PlayerPair.bye());

    assertTrue(game.isFinished(), "Game with BYE should be finished");
    assertEquals(teamA, game.getWinner(), "Team A should win against BYE");
  }

  @Test
  void testForfeit_afterCompletedMatch_forfeitTakesPrecedence() {
    MatchFormat standardFormat = new MatchFormat(null, 2, 6, false, true);
    PlayerPair  teamA          = new PlayerPair("Alice", "Bob", 1);
    PlayerPair  teamB          = new PlayerPair("Charlie", "Dave", 2);

    Game game = new Game();
    game.setFormat(standardFormat);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Score normalScore = new Score();
    normalScore.addSetScore(6, 4);
    normalScore.addSetScore(6, 2);
    game.setScore(normalScore);

    assertEquals(teamA, game.getWinner(), "Team A is the winner");

    normalScore.markAsForfeit(TeamSide.TEAM_A);
    game.setScore(normalScore);

    assertEquals(teamB, game.getWinner(), "Team B should win if Team A is marked as forfeiter");
  }

  @Test
  void testSuperTieBreakWinner() {
    PlayerPair teamA = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair teamB = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    Game       game  = new Game(new MatchFormat(1L, 2, 6, true, false));
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score score = new Score();
    score.addSetScore(6, 0); // A wins first set
    score.addSetScore(0, 6); // B wins second set
    SetScore superTie = new SetScore(0, 1); // B wins super tie-break
    superTie.setTieBreakTeamA(4);
    superTie.setTieBreakTeamB(10);
    score.getSets().add(superTie);
    game.setScore(score);
    assertTrue(game.isFinished());
    assertEquals(teamB, game.getWinner());
    // Now test for teamA win
    Game game2 = new Game(new MatchFormat(1L, 2, 6, true, false));
    game2.setTeamA(teamA);
    game2.setTeamB(teamB);
    Score score2 = new Score();
    score2.addSetScore(6, 0);
    score2.addSetScore(0, 6);
    SetScore superTie2 = new SetScore(1, 0);
    superTie2.setTieBreakTeamA(10);
    superTie2.setTieBreakTeamB(4);
    score2.getSets().add(superTie2);
    game2.setScore(score2);
    assertTrue(game2.isFinished());
    assertEquals(teamA, game2.getWinner());
  }

  @ParameterizedTest
  @CsvSource(value = {
      // Format: setScores|tieBreakA|tieBreakB|expectedWinnerSide
      "6-0,0-6,0-1|0|10|TEAM_B",
      "6-0,0-6,1-0|10|0|TEAM_A",
      "6-0,0-6,0-1|0|11|TEAM_B",
      "6-0,0-6,1-0|11|0|TEAM_A",
      "6-0,0-6,0-1|0|15|TEAM_B",
      "6-0,0-6,1-0|15|0|TEAM_A",
      // Edge: not finished
      "6-0,0-6,0-0|0|0|null",
      "6-0,0-6,0-1|0|9|null",
      "6-0,0-6,1-0|9|0|null"
  }, delimiter = '|')
  void testWinnerSideIsSetForSuperTieBreakWin_param(String setScores, String tieBreakA, String tieBreakB, String expectedWinnerSide) {
    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false); // 2 sets to win, super tie-break in final set
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score    score = new Score();
    String[] sets  = setScores.split(",");
    for (String set : sets) {
      String[] ab = set.split("-");
      int      a  = Integer.parseInt(ab[0]);
      int      b  = Integer.parseInt(ab[1]);
      score.getSets().add(new SetScore(a, b));
    }
    // Set tie-break points for last set
    int tieA = Integer.parseInt(tieBreakA);
    int tieB = Integer.parseInt(tieBreakB);
    score.getSets().get(score.getSets().size() - 1).setTieBreakTeamA(tieA);
    score.getSets().get(score.getSets().size() - 1).setTieBreakTeamB(tieB);
    game.setScore(score);
    if ("TEAM_A".equals(expectedWinnerSide)) {
      assertEquals(TeamSide.TEAM_A, game.getWinnerSide());
    } else if ("TEAM_B".equals(expectedWinnerSide)) {
      assertEquals(TeamSide.TEAM_B, game.getWinnerSide());
    } else {
      assertNull(game.getWinnerSide());
    }
  }

  @Test
  void testSuperTieBreakWithZeroGamesButValidTieBreakScore() {
    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score score = new Score();
    score.addSetScore(6, 0);
    score.addSetScore(0, 6);
    SetScore superTie = new SetScore(0, 0); // games = 0-0
    superTie.setTieBreakTeamA(4);
    superTie.setTieBreakTeamB(10); // teamB wins
    score.getSets().add(superTie);
    game.setScore(score);
    assertTrue(game.isFinished(), "Game should be finished when super tie-break is won, even if games are 0-0");
    assertEquals(teamB, game.getWinner(), "Team B should be the winner");
    assertEquals(TeamSide.TEAM_B, game.getWinnerSide(), "Winner side should be TEAM_B");
  }

  @Test
  void testSuperTieBreakWithZeroOneGamesAndValidTieBreakScore() {
    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score score = new Score();
    score.addSetScore(6, 0);
    score.addSetScore(0, 6);
    SetScore superTie = new SetScore(0, 1); // games = 0-1
    superTie.setTieBreakTeamA(1);
    superTie.setTieBreakTeamB(10); // teamB wins
    score.getSets().add(superTie);
    game.setScore(score);
    assertTrue(game.isFinished(), "Game should be finished when super tie-break is won, even if games are 0-1");
    assertEquals(teamB, game.getWinner(), "Team B should be the winner");
    assertEquals(TeamSide.TEAM_B, game.getWinnerSide(), "Winner side should be TEAM_B");
  }

  @Test
  void testSuperTieBreakWithOneZeroGamesAndValidTieBreakScore_TeamA() {
    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score score = new Score();
    score.addSetScore(6, 0);
    score.addSetScore(2, 6);
    SetScore superTie = new SetScore(1, 0); // games = 1-0
    superTie.setTieBreakTeamA(10);
    superTie.setTieBreakTeamB(2); // teamA wins
    score.getSets().add(superTie);
    game.setScore(score);
    assertTrue(game.isFinished(), "Game should be finished when super tie-break is won, even if games are 1-0");
    assertEquals(teamA, game.getWinner(), "Team A should be the winner");
    assertEquals(TeamSide.TEAM_A, game.getWinnerSide(), "Winner side should be TEAM_A");
  }

  @Test
  void testSuperTieBreakWithZeroZeroGamesAndValidTieBreakScore() {
    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score score = new Score();
    score.addSetScore(6, 0);
    score.addSetScore(0, 6);
    SetScore superTie = new SetScore(0, 0); // games = 0-0
    superTie.setTieBreakTeamA(10);
    superTie.setTieBreakTeamB(4); // teamA wins
    score.getSets().add(superTie);
    game.setScore(score);
    assertTrue(game.isFinished(), "Game should be finished when super tie-break is won, even if games are 0-0");
    assertEquals(teamA, game.getWinner(), "Team A should be the winner");
    assertEquals(TeamSide.TEAM_A, game.getWinnerSide(), "Winner side should be TEAM_A");
  }

  @Test
  void testSuperTieBreakWithZeroZeroGamesAndValidTieBreakScore_realFrontCase() {
    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score score = new Score();
    score.addSetScore(6, 2);
    score.addSetScore(2, 6);
    SetScore superTie = new SetScore(0, 0); // games = 0-0
    superTie.setTieBreakTeamA(10);
    superTie.setTieBreakTeamB(4); // teamA wins
    score.getSets().add(superTie);
    game.setScore(score);
    assertTrue(game.isFinished(), "Game should be finished when super tie-break is won, even if games are 0-0");
    assertEquals(teamA, game.getWinner(), "Team A should be the winner");
    assertEquals(TeamSide.TEAM_A, game.getWinnerSide(), "Winner side should be TEAM_A");
  }

  @Test
  void testSuperTieBreakWithZeroZeroGamesAndValidTieBreakScore_realFrontCase_variant() {
    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score score = new Score();
    score.addSetScore(6, 2);
    score.addSetScore(2, 6);
    SetScore superTie = new SetScore(0, 0); // games = 0-0
    superTie.setTieBreakTeamA(4);
    superTie.setTieBreakTeamB(10); // teamB wins
    score.getSets().add(superTie);
    game.setScore(score);
    assertTrue(game.isFinished(), "Game should be finished when super tie-break is won, even if games are 0-0");
    assertEquals(teamB, game.getWinner(), "Team B should be the winner");
    assertEquals(TeamSide.TEAM_B, game.getWinnerSide(), "Winner side should be TEAM_B");
  }

  @Test
  void testSuperTieBreakWithZeroZeroGamesAndValidTieBreakScore_realFrontCase_teamB() {
    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score score = new Score();
    score.addSetScore(6, 2);
    score.addSetScore(2, 6);
    SetScore superTie = new SetScore(0, 0); // games = 0-0
    superTie.setTieBreakTeamA(4);
    superTie.setTieBreakTeamB(10); // teamB wins
    score.getSets().add(superTie);
    game.setScore(score);
    assertTrue(game.isFinished(), "Game should be finished when super tie-break is won, even if games are 0-0");
    assertEquals(teamB, game.getWinner(), "Team B should be the winner");
    assertEquals(TeamSide.TEAM_B, game.getWinnerSide(), "Winner side should be TEAM_B");
  }

  @Test
  void testSuperTieBreakWithZeroZeroGamesAndInvalidTieBreakScore_notFinished() {
    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score score = new Score();
    score.addSetScore(6, 2);
    score.addSetScore(2, 6);
    SetScore superTie = new SetScore(0, 0); // games = 0-0
    superTie.setTieBreakTeamA(7);
    superTie.setTieBreakTeamB(6); // not enough for win
    score.getSets().add(superTie);
    game.setScore(score);
    assertFalse(game.isFinished(), "Game should not be finished if tie-break is not won");
    assertNull(game.getWinner(), "No winner should be set");
    assertNull(game.getWinnerSide(), "Winner side should be null");
  }

  @Test
  void testSuperTieBreakWithZeroZeroGamesAndNoTieBreakScore_notFinished() {
    PlayerPair  teamA  = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair  teamB  = new PlayerPair(new Player("B1"), new Player("B2"), 2);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);
    Game        game   = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score score = new Score();
    score.addSetScore(6, 2);
    score.addSetScore(2, 6);
    SetScore superTie = new SetScore(0, 0); // games = 0-0
    // No tieBreakTeamA or tieBreakTeamB set
    score.getSets().add(superTie);
    game.setScore(score);
    assertFalse(game.isFinished(), "Game should not be finished if tie-break is not set");
    assertNull(game.getWinner(), "No winner should be set");
    assertNull(game.getWinnerSide(), "Winner side should be null");
  }

}
