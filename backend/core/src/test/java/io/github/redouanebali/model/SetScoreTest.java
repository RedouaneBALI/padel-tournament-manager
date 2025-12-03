package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SetScoreTest {

  @Test
  void constructor_withoutTieBreak_shouldInitializeCorrectly() {
    SetScore setScore = new SetScore(6, 4);

    assertEquals(6, setScore.getTeamAScore());
    assertEquals(4, setScore.getTeamBScore());
  }

  @Test
  void constructor_withTieBreak_shouldInitializeCorrectly() {
    SetScore setScore = new SetScore(7, 6, 7, 5);

    assertEquals(7, setScore.getTeamAScore());
    assertEquals(6, setScore.getTeamBScore());
    assertEquals(7, setScore.getTieBreakTeamA());
    assertEquals(5, setScore.getTieBreakTeamB());
  }

  @ParameterizedTest
  @CsvSource(nullValues = "NULL", value = {
      "6, 4, NULL, NULL, 6, 4, NULL, NULL, true",
      "7, 6, 7, 5, 7, 6, 7, 5, true",
      "6, 4, NULL, NULL, 6, 3, NULL, NULL, false",
      "7, 6, 7, 5, 7, 6, 7, 4, false",
      "6, 4, NULL, NULL, 6, 4, 7, 5, false"
  })
  void equals_shouldCompareAllFields(Integer a1, Integer b1, Integer tieA1, Integer tieB1,
                                     Integer a2, Integer b2, Integer tieA2, Integer tieB2,
                                     boolean expected) {
    SetScore score1 = new SetScore(a1, b1, tieA1, tieB1);
    SetScore score2 = new SetScore(a2, b2, tieA2, tieB2);

    if (expected) {
      assertEquals(score1, score2);
    } else {
      assertNotEquals(score1, score2);
    }
  }

  @Test
  void equals_sameObject_shouldReturnTrue() {
    SetScore score = new SetScore(6, 4);

    assertEquals(score, score);
  }

  @Test
  void equals_nullObject_shouldReturnFalse() {
    SetScore score = new SetScore(6, 4);

    assertNotEquals(null, score);
  }

  @Test
  void equals_differentClass_shouldReturnFalse() {
    SetScore score = new SetScore(6, 4);

    assertNotEquals(score, "6-4");
  }

  @Test
  void hashCode_shouldBeConsistent() {
    SetScore score1 = new SetScore(6, 4, null, null);
    SetScore score2 = new SetScore(6, 4, null, null);

    assertEquals(score1.hashCode(), score2.hashCode());
  }

  @Test
  void hashCode_withTieBreak_shouldBeConsistent() {
    SetScore score1 = new SetScore(7, 6, 7, 5);
    SetScore score2 = new SetScore(7, 6, 7, 5);

    assertEquals(score1.hashCode(), score2.hashCode());
  }

  @Test
  void hashCode_differentScores_shouldBeDifferent() {
    SetScore score1 = new SetScore(6, 4, null, null);
    SetScore score2 = new SetScore(6, 3, null, null);

    assertNotEquals(score1.hashCode(), score2.hashCode());
  }

  @ParameterizedTest
  @CsvSource(nullValues = "NULL", value = {
      "6, 4, NULL, NULL",
      "7, 5, NULL, NULL",
      "0, 0, NULL, NULL",
      "7, 6, 7, 5",
      "6, 6, 10, 8"
  })
  void setters_shouldUpdateValues(Integer a, Integer b, Integer tieA, Integer tieB) {
    SetScore score = new SetScore();

    score.setTeamAScore(a);
    score.setTeamBScore(b);
    score.setTieBreakTeamA(tieA);
    score.setTieBreakTeamB(tieB);

    assertEquals(a, score.getTeamAScore());
    assertEquals(b, score.getTeamBScore());
    assertEquals(tieA, score.getTieBreakTeamA());
    assertEquals(tieB, score.getTieBreakTeamB());
  }
}

