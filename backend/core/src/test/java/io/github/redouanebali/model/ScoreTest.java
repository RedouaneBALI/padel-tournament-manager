package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScoreTest {

  @Test
  void testAddSetScore() {
    Score score = new Score();
    score.addSetScore(6, 4);
    score.addSetScore(7, 5);

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
}
