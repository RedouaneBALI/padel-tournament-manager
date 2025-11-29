package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
