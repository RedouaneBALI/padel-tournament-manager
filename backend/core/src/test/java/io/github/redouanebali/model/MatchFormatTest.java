package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MatchFormatTest {

  @ParameterizedTest
  @CsvSource({
      "6, 6",
      "6, 5",
      "4, 4",
      "4, 3"
  })
  void setTieBreakAt_shouldAcceptValidValues(int gamesPerSet, int tieBreakAt) {
    MatchFormat format = new MatchFormat();
    format.setGamesPerSet(gamesPerSet);
    format.setTieBreakAt(tieBreakAt);
    assertEquals(tieBreakAt, format.getTieBreakAt());
  }

  @ParameterizedTest
  @CsvSource({
      "6, 4",
      "6, 7",
      "4, 2",
      "4, 6"
  })
  void setTieBreakAt_shouldRejectInvalidValues(int gamesPerSet, int tieBreakAt) {
    MatchFormat format = new MatchFormat();
    format.setGamesPerSet(gamesPerSet);

    assertThrows(IllegalArgumentException.class, () -> format.setTieBreakAt(tieBreakAt));
  }

  @Test
  void setGamesPerSet_shouldAutoAdjustTieBreakAt() {
    MatchFormat format = new MatchFormat();
    format.setGamesPerSet(6);
    assertEquals(6, format.getTieBreakAt());

    format.setGamesPerSet(4);
    assertEquals(4, format.getTieBreakAt());
  }

  @ParameterizedTest
  @CsvSource({
      "2, 6, true, false, 2, 6, true, false",
      "2, 4, false, true, 2, 4, false, true",
      "1, 9, false, false, 1, 9, false, false"
  })
  void constructor_shouldInitializeCorrectly(
      int numberOfSetsToWin, int gamesPerSet, boolean superTie, boolean advantage,
      int expectedSets, int expectedGames, boolean expectedSuperTie, boolean expectedAdvantage) {

    MatchFormat format = new MatchFormat(1L, numberOfSetsToWin, gamesPerSet, superTie, advantage);

    assertEquals(expectedSets, format.getNumberOfSetsToWin());
    assertEquals(expectedGames, format.getGamesPerSet());
    assertEquals(expectedSuperTie, format.isSuperTieBreakInFinalSet());
    assertEquals(expectedAdvantage, format.isAdvantage());
    assertEquals(gamesPerSet, format.getTieBreakAt());
  }

  @Test
  void constructorWithTieBreakAt_shouldSetCorrectly() {
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false, 5);

    assertEquals(2, format.getNumberOfSetsToWin());
    assertEquals(6, format.getGamesPerSet());
    assertEquals(5, format.getTieBreakAt());
  }

  @Test
  void constructorWithTieBreakAt_shouldThrowOnInvalidValue() {
    assertThrows(IllegalArgumentException.class,
                 () -> new MatchFormat(1L, 2, 6, true, false, 4));
  }
}

