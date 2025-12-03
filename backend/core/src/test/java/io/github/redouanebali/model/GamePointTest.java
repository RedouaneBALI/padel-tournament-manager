package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GamePointTest {

  @Test
  void testGamePointProgression() {
    assertEquals(GamePoint.QUINZE, GamePoint.ZERO.next());
    assertEquals(GamePoint.TRENTE, GamePoint.QUINZE.next());
    assertEquals(GamePoint.QUARANTE, GamePoint.TRENTE.next());
    assertEquals(GamePoint.AVANTAGE, GamePoint.QUARANTE.next());
    assertEquals(GamePoint.AVANTAGE, GamePoint.AVANTAGE.next());
  }

  @Test
  void testGamePointRegression() {
    assertEquals(GamePoint.ZERO, GamePoint.ZERO.previous());
    assertEquals(GamePoint.ZERO, GamePoint.QUINZE.previous());
    assertEquals(GamePoint.QUINZE, GamePoint.TRENTE.previous());
    assertEquals(GamePoint.TRENTE, GamePoint.QUARANTE.previous());
    assertEquals(GamePoint.QUARANTE, GamePoint.AVANTAGE.previous());
  }

  @Test
  void testGamePointDisplay() {
    assertEquals("0", GamePoint.ZERO.getDisplay());
    assertEquals("15", GamePoint.QUINZE.getDisplay());
    assertEquals("30", GamePoint.TRENTE.getDisplay());
    assertEquals("40", GamePoint.QUARANTE.getDisplay());
    assertEquals("A", GamePoint.AVANTAGE.getDisplay());
  }
}

