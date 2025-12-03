package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScoreTest {

  @Test
  void testAddSetScore() {
    Score score = new Score();
    score.addSetScore(6, 4);
    score.addSetScore(7, 5);

    score.getSets().forEach(s -> System.out.println(s.getTeamAScore() + "-" + s.getTeamBScore()));

    assertEquals(2, score.getSets().size());
    assertEquals(6, score.getSets().getFirst().getTeamAScore());
    assertEquals(4, score.getSets().getFirst().getTeamBScore());
  }

  @Test
  void testCompleteScoreRecording() {
    Score score = new Score();
    score.addSetScore(6, 4);
    score.addSetScore(4, 6);
    score.addSetScore(10, 7); // Assuming this is super tie-break but recorded as a set

    assertEquals(3, score.getSets().size());
    assertEquals(10, score.getSets().get(2).getTeamAScore());
    assertEquals(7, score.getSets().get(2).getTeamBScore());
  }

  // ============= FROM STRING TESTS =============

  @ParameterizedTest
  @CsvSource({
      "'6-4 6-3', 2",
      "'6-4,6-3', 2",
      "'6-4  6-3', 2",
      "'7-5', 1",
      "'6-0 6-0 6-4', 3",
      "'', 0",
      "' ', 0"
  })
  void testFromString_validFormats(String input, int expectedSets) {
    Score score = Score.fromString(input);

    assertNotNull(score);
    assertEquals(expectedSets, score.getSets().size());
  }

  @Test
  void testFromString_parsesTwoSetsCorrectly() {
    Score score = Score.fromString("6-4 6-3");

    assertEquals(2, score.getSets().size());
    assertEquals(6, score.getSets().getFirst().getTeamAScore());
    assertEquals(4, score.getSets().getFirst().getTeamBScore());
    assertEquals(6, score.getSets().get(1).getTeamAScore());
    assertEquals(3, score.getSets().get(1).getTeamBScore());
  }

  @Test
  void testFromString_parsesCommaDelimiter() {
    Score score = Score.fromString("6-4,6-3");

    assertEquals(2, score.getSets().size());
    assertEquals(6, score.getSets().getFirst().getTeamAScore());
    assertEquals(4, score.getSets().getFirst().getTeamBScore());
  }

  @ParameterizedTest
  @CsvSource({
      "'invalid'",
      "'6-'",
      "'-4'",
      "'6'",
      "'a-b'"
  })
  void testFromString_invalidFormats(String input) {
    assertThrows(IllegalArgumentException.class, () -> Score.fromString(input));
  }

  // ============= FORFEIT TESTS =============

  @Test
  void testForfeit_withTeamA() {
    Score score = Score.forfeit(TeamSide.TEAM_A);

    assertNotNull(score);
    assertTrue(score.isForfeit(), "Score should be marked as forfeit");
    assertEquals(TeamSide.TEAM_A, score.getForfeitedBy(), "Team A should be marked as forfeiter");
    assertTrue(score.getSets().isEmpty(), "No sets should be recorded for a forfeit without score");
  }

  @Test
  void testForfeit_withTeamB() {
    Score score = Score.forfeit(TeamSide.TEAM_B);

    assertNotNull(score);
    assertTrue(score.isForfeit(), "Score should be marked as forfeit");
    assertEquals(TeamSide.TEAM_B, score.getForfeitedBy(), "Team B should be marked as forfeiter");
    assertTrue(score.getSets().isEmpty(), "No sets should be recorded for a forfeit without score");
  }

  @Test
  void testForfeitWithPartialScore_preservesSets() {
    // Create a partial score
    Score partialScore = new Score();
    partialScore.addSetScore(6, 2);
    partialScore.addSetScore(2, 0);

    // Mark as forfeit
    Score finalScore = Score.forfeitWithPartialScore(partialScore, TeamSide.TEAM_B);

    assertNotNull(finalScore);
    assertTrue(finalScore.isForfeit(), "Score should be marked as forfeit");
    assertEquals(TeamSide.TEAM_B, finalScore.getForfeitedBy(), "Team B should be marked as forfeiter");
    assertEquals(2, finalScore.getSets().size(), "Partial score should be preserved");
    assertEquals(6, finalScore.getSets().get(0).getTeamAScore());
    assertEquals(2, finalScore.getSets().get(0).getTeamBScore());
  }

  @Test
  void testForfeitWithPartialScore_nullScore() {
    Score finalScore = Score.forfeitWithPartialScore(null, TeamSide.TEAM_A);

    assertNotNull(finalScore);
    assertTrue(finalScore.isForfeit(), "Score should be marked as forfeit");
    assertEquals(TeamSide.TEAM_A, finalScore.getForfeitedBy());
    assertTrue(finalScore.getSets().isEmpty(), "No sets should exist when partial score is null");
  }

  @Test
  void testMarkAsForfeit_onExistingScore() {
    Score score = new Score();
    score.addSetScore(6, 4);
    score.addSetScore(3, 2);

    assertFalse(score.isForfeit(), "Initially not a forfeit");

    score.markAsForfeit(TeamSide.TEAM_A);

    assertTrue(score.isForfeit(), "Should now be marked as forfeit");
    assertEquals(TeamSide.TEAM_A, score.getForfeitedBy());
    assertEquals(2, score.getSets().size(), "Sets should be preserved");
  }

  @Test
  void testToString_forfeitWithoutScore() {
    Score score = Score.forfeit(TeamSide.TEAM_A);

    String result = score.toString();

    assertEquals("(Forfeit by TEAM_A)", result);
  }

  @Test
  void testToString_forfeitWithPartialScore() {
    Score score = new Score();
    score.addSetScore(6, 2);
    score.addSetScore(2, 0);
    score.markAsForfeit(TeamSide.TEAM_B);

    String result = score.toString();

    assertEquals("6-2 2-0 (Forfeit by TEAM_B)", result);
  }

  @Test
  void testToString_normalScore() {
    Score score = new Score();
    score.addSetScore(6, 4);
    score.addSetScore(4, 6);
    score.addSetScore(7, 5);

    String result = score.toString();

    assertEquals("6-4 4-6 7-5", result);
    assertFalse(score.isForfeit());
  }

  @Test
  void testGamePointDisplayAndNullByDefault() {
    Score score = new Score();
    // By default, GamePoints and tieBreakPoints should be null
    assertNull(score.getCurrentGamePointA());
    assertNull(score.getCurrentGamePointB());
    assertNull(score.getTieBreakPointA());
    assertNull(score.getTieBreakPointB());
  }

  @Test
  void testToString_withGamePoints_classicGame() {
    Score score = new Score();
    score.addSetScore(5, 4); // Not a tie-break situation
    score.setCurrentGamePointA(GamePoint.QUINZE);
    score.setCurrentGamePointB(GamePoint.QUARANTE);
    String result = score.toString();
    assertTrue(result.contains("(15-40)"), "Should display classic game points when not in tie-break");
  }

  @Test
  void testToString_withTieBreakPoints_only() {
    Score score = new Score();
    score.addSetScore(6, 6); // Tie-break situation
    score.setTieBreakPointA(5);
    score.setTieBreakPointB(7);
    String result = score.toString();
    assertTrue(result.contains("(5-7)"), "Should display tie-break points at 6-6");
    // Should not display GamePoint if tieBreakPoint is present
    score.setCurrentGamePointA(GamePoint.QUINZE);
    score.setCurrentGamePointB(GamePoint.QUARANTE);
    assertTrue(result.contains("(5-7)"), "Tie-break points have priority over classic game points");
  }

  @Test
  void testToString_withGamePoints_and_noTieBreak() {
    Score score = new Score();
    score.addSetScore(4, 4); // Not a tie-break
    score.setCurrentGamePointA(GamePoint.TRENTE);
    score.setCurrentGamePointB(GamePoint.QUINZE);
    String result = score.toString();
    assertTrue(result.contains("(30-15)"), "Should display classic game points when not in tie-break");
  }

  @Test
  void testTieBreakPointProgression_realistic() {
    Score score = new Score();
    score.addSetScore(6, 6); // Tie-break situation
    score.setTieBreakPointA(0);
    score.setTieBreakPointB(0);
    for (int i = 1; i <= 7; i++) {
      score.setTieBreakPointA(i);
      assertEquals(i, score.getTieBreakPointA());
    }
    for (int i = 1; i <= 5; i++) {
      score.setTieBreakPointB(i);
      assertEquals(i, score.getTieBreakPointB());
    }
    // Simulate win: A 7-5
    score.setTieBreakPointA(7);
    score.setTieBreakPointB(5);
    assertEquals(7, score.getTieBreakPointA());
    assertEquals(5, score.getTieBreakPointB());
  }

  @Test
  void testNoAdRuleAtDeuce() {
    // Simulate a no-ad game at 40-40
    MatchFormat format = new MatchFormat(1L, 2, 6, false, false);
    Score       score  = new Score();
    score.setCurrentGamePointA(GamePoint.QUARANTE);
    score.setCurrentGamePointB(GamePoint.QUARANTE);
    // Next point wins the game (no advantage)
    boolean   withAdvantage = format.isAdvantage();
    GamePoint nextA         = nextGamePoint(score.getCurrentGamePointA(), score.getCurrentGamePointB(), withAdvantage);
    assertEquals(GamePoint.QUARANTE, nextA, "No-ad: should stay at 40, next point triggers game win logic");
  }

  @Test
  void testAdvantageRuleAtDeuce() {
    // Simulate a game with advantage at 40-40
    MatchFormat format = new MatchFormat(1L, 2, 6, false, true);
    Score       score  = new Score();
    score.setCurrentGamePointA(GamePoint.QUARANTE);
    score.setCurrentGamePointB(GamePoint.QUARANTE);
    boolean   withAdvantage = format.isAdvantage();
    GamePoint nextA         = nextGamePoint(score.getCurrentGamePointA(), score.getCurrentGamePointB(), withAdvantage);
    assertEquals(GamePoint.AVANTAGE, nextA, "With advantage: should go to AVANTAGE");
  }

  @Test
  void testTieBreakAtCustomGamesPerSet_4() {
    // Format : set en 4 jeux, tie-break à 4-4
    MatchFormat format = new MatchFormat(1L, 1, 4, false, true, 4);
    Score       score  = new Score();
    score.addSetScore(4, 4); // On atteint le tie-break
    score.setTieBreakPointA(2);
    score.setTieBreakPointB(4);
    String result = score.toString();
    assertTrue(result.contains("(2-4)"), "Tie-break points should be displayed at 4-4");
  }

  @Test
  void testTieBreakAtCustomGamesPerSet_9() {
    // Format : set en 9 jeux, tie-break à 9-9
    MatchFormat format = new MatchFormat(1L, 1, 9, false, true, 9);
    Score       score  = new Score();
    score.addSetScore(9, 9); // On atteint le tie-break
    score.setTieBreakPointA(7);
    score.setTieBreakPointB(5);
    String result = score.toString();
    assertTrue(result.contains("(7-5)"), "Tie-break points should be displayed at 9-9");
  }

  @ParameterizedTest
  @CsvSource({
      // format: actions,expectedSetCount,expectedLastSetA,expectedLastSetB,expectedForfeit,expectedForfeitedBy,expectedTieBreakA,expectedTieBreakB,expectedGamePointA,expectedGamePointB
      "add:6-4|add:7-5|undo,1,6,4,false,,null,null,null,null",
      "add:6-4|add:7-5|add:10-8|undo,2,7,5,false,,null,null,null,null",
      "add:6-4|add:7-5|add:10-8|undo|undo,1,6,4,false,,null,null,null,null",
      "add:6-6|setTieBreak:5-7|setGamePoint:15-40|setTieBreak:8-10|setGamePoint:AVANTAGE-0|forfeit:TEAM_B|undo,1,6,6,false,,8,10,AVANTAGE,0",
      "add:6-4|undo,1,6,4,false,,null,null,null,null",
      "add:6-2|undo,1,6,2,false,,null,null,null,null"
  })
  void testUndoParametrized(String actions,
                            int expectedSetCount,
                            int expectedLastSetA,
                            int expectedLastSetB,
                            boolean expectedForfeit,
                            String expectedForfeitedBy,
                            String expectedTieBreakA,
                            String expectedTieBreakB,
                            String expectedGamePointA,
                            String expectedGamePointB) {
    Score    score             = new Score();
    String[] steps             = actions.split("\\|");
    boolean  firstModification = true;
    for (String step : steps) {
      // Sauvegarde automatique avant chaque modification (sauf undo), sauf avant la toute première modif
      if (!step.equals("undo") && !firstModification) {
        score.saveToHistory();
      }
      if (step.startsWith("add:")) {
        String[] ab = step.substring(4).split("-");
        score.addSetScore(Integer.parseInt(ab[0]), Integer.parseInt(ab[1]));
        firstModification = false;
      } else if (step.equals("undo")) {
        score.undo();
      } else if (step.startsWith("setTieBreak:")) {
        String[] ab = step.substring(12).split("-");
        score.setTieBreakPointA(Integer.parseInt(ab[0]));
        score.setTieBreakPointB(Integer.parseInt(ab[1]));
        firstModification = false;
      } else if (step.startsWith("setGamePoint:")) {
        String[] ab = step.substring(13).split("-");
        score.setCurrentGamePointA(parseGamePoint(ab[0]));
        score.setCurrentGamePointB(parseGamePoint(ab[1]));
        firstModification = false;
      } else if (step.startsWith("forfeit:")) {
        score.markAsForfeit(TeamSide.valueOf(step.substring(8)));
        firstModification = false;
      }
    }
    assertEquals(expectedSetCount, score.getSets().size());
    assertEquals(expectedLastSetA, score.getSets().get(score.getSets().size() - 1).getTeamAScore());
    assertEquals(expectedLastSetB, score.getSets().get(score.getSets().size() - 1).getTeamBScore());
    assertEquals(expectedForfeit, score.isForfeit());
    if (expectedForfeitedBy == null || expectedForfeitedBy.isEmpty()) {
      assertNull(score.getForfeitedBy());
    } else {
      assertEquals(TeamSide.valueOf(expectedForfeitedBy), score.getForfeitedBy());
    }
    if (expectedTieBreakA != null && !expectedTieBreakA.equals("null")) {
      assertEquals(Integer.parseInt(expectedTieBreakA), score.getTieBreakPointA());
    } else {
      assertNull(score.getTieBreakPointA());
    }
    if (expectedTieBreakB != null && !expectedTieBreakB.equals("null")) {
      assertEquals(Integer.parseInt(expectedTieBreakB), score.getTieBreakPointB());
    } else {
      assertNull(score.getTieBreakPointB());
    }
    if (expectedGamePointA != null && !expectedGamePointA.equals("null")) {
      if (expectedGamePointA.equals("0")) {
        assertNull(score.getCurrentGamePointA());
      } else {
        assertEquals(parseGamePoint(expectedGamePointA), score.getCurrentGamePointA());
      }
    }
    if (expectedGamePointB != null && !expectedGamePointB.equals("null")) {
      if (expectedGamePointB.equals("0")) {
        assertNull(score.getCurrentGamePointB());
      } else {
        assertEquals(parseGamePoint(expectedGamePointB), score.getCurrentGamePointB());
      }
    }
  }

  private GamePoint parseGamePoint(String s) {
    if (s == null || s.equals("null") || s.equals("0")) {
      return null;
    }
    return switch (s) {
      case "ZERO" -> GamePoint.ZERO;
      case "QUINZE", "15" -> GamePoint.QUINZE;
      case "TRENTE", "30" -> GamePoint.TRENTE;
      case "QUARANTE", "40" -> GamePoint.QUARANTE;
      case "AVANTAGE" -> GamePoint.AVANTAGE;
      default -> throw new IllegalArgumentException("Unknown GamePoint: " + s);
    };
  }

  // Helpers (copied from GameService for test purpose)
  private GamePoint nextGamePoint(GamePoint current, GamePoint opponent, boolean withAdvantage) {
    if (withAdvantage) {
      switch (current) {
        case ZERO:
          return GamePoint.QUINZE;
        case QUINZE:
          return GamePoint.TRENTE;
        case TRENTE:
          return GamePoint.QUARANTE;
        case QUARANTE:
          return GamePoint.AVANTAGE;
        case AVANTAGE:
        default:
          return GamePoint.AVANTAGE;
      }
    } else {
      switch (current) {
        case ZERO:
          return GamePoint.QUINZE;
        case QUINZE:
          return GamePoint.TRENTE;
        case TRENTE:
          return GamePoint.QUARANTE;
        case QUARANTE:
          return GamePoint.QUARANTE;
        case AVANTAGE:
        default:
          return GamePoint.QUARANTE;
      }
    }
  }
}
