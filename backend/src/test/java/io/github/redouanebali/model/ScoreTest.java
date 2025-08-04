package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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

}
