package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GamePointTest {

  @Test
  void testGamePointProgression() {
    assertEquals(GamePoint.QUINZE, nextGamePoint(GamePoint.ZERO));
    assertEquals(GamePoint.TRENTE, nextGamePoint(GamePoint.QUINZE));
    assertEquals(GamePoint.QUARANTE, nextGamePoint(GamePoint.TRENTE));
    assertEquals(GamePoint.AVANTAGE, nextGamePoint(GamePoint.QUARANTE));
    assertEquals(GamePoint.AVANTAGE, nextGamePoint(GamePoint.AVANTAGE));
  }

  @Test
  void testGamePointRegression() {
    assertEquals(GamePoint.ZERO, prevGamePoint(GamePoint.ZERO));
    assertEquals(GamePoint.ZERO, prevGamePoint(GamePoint.QUINZE));
    assertEquals(GamePoint.QUINZE, prevGamePoint(GamePoint.TRENTE));
    assertEquals(GamePoint.TRENTE, prevGamePoint(GamePoint.QUARANTE));
    assertEquals(GamePoint.QUARANTE, prevGamePoint(GamePoint.AVANTAGE));
  }

  @Test
  void testGamePointDisplay() {
    assertEquals("0", GamePoint.ZERO.getDisplay());
    assertEquals("15", GamePoint.QUINZE.getDisplay());
    assertEquals("30", GamePoint.TRENTE.getDisplay());
    assertEquals("40", GamePoint.QUARANTE.getDisplay());
    assertEquals("A", GamePoint.AVANTAGE.getDisplay());
  }

  // Helpers (copied from GameService for test purpose)
  private GamePoint nextGamePoint(GamePoint current) {
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
  }

  private GamePoint prevGamePoint(GamePoint current) {
    switch (current) {
      case AVANTAGE:
        return GamePoint.QUARANTE;
      case QUARANTE:
        return GamePoint.TRENTE;
      case TRENTE:
        return GamePoint.QUINZE;
      case QUINZE:
        return GamePoint.ZERO;
      case ZERO:
      default:
        return GamePoint.ZERO;
    }
  }
}

