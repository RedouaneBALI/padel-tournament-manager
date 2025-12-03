package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlayerPairTest {

  @Test
  void bye_shouldCreateByePair() {
    PlayerPair bye = PlayerPair.bye();

    assertTrue(bye.isBye());
    assertFalse(bye.isQualifier());
    assertEquals(PairType.BYE, bye.getType());
    assertEquals(Integer.MAX_VALUE, bye.getSeed());
    assertEquals("BYE", bye.toString());
  }

  @ParameterizedTest
  @CsvSource({
      "1, Q1",
      "2, Q2",
      "3, Q3",
      "10, Q10"
  })
  void qualifier_shouldCreateQualifierPairWithIndex(int index, String expectedName) {
    PlayerPair qualifier = PlayerPair.qualifier(index);

    assertTrue(qualifier.isQualifier());
    assertFalse(qualifier.isBye());
    assertEquals(PairType.QUALIFIER, qualifier.getType());
    assertEquals(Integer.MAX_VALUE, qualifier.getSeed());
    assertEquals(index, qualifier.getQualifierIndex());
    assertEquals(expectedName, qualifier.toString());
  }

  @Test
  void constructor_shouldCreateNormalPair() {
    Player p1 = new Player("John");
    Player p2 = new Player("Jane");

    PlayerPair pair = new PlayerPair(p1, p2, 5);

    assertEquals(p1, pair.getPlayer1());
    assertEquals(p2, pair.getPlayer2());
    assertEquals(5, pair.getSeed());
    assertEquals(PairType.NORMAL, pair.getType());
    assertFalse(pair.isBye());
    assertFalse(pair.isQualifier());
  }

  @Test
  void stringConstructor_shouldCreatePlayersFromNames() {
    PlayerPair pair = new PlayerPair("Alice", "Bob", 3);

    assertEquals("Alice", pair.getPlayer1().getName());
    assertEquals("Bob", pair.getPlayer2().getName());
    assertEquals(3, pair.getSeed());
    assertEquals("Alice / Bob", pair.toString());
  }

  @Test
  void toString_shouldFormatNormalPairCorrectly() {
    Player     p1   = new Player("Player1");
    Player     p2   = new Player("Player2");
    PlayerPair pair = new PlayerPair(p1, p2, 1);

    assertEquals("Player1 / Player2", pair.toString());
  }

  @Test
  void equals_shouldCompareById() {
    PlayerPair pair1 = new PlayerPair();
    pair1.setId(1L);

    PlayerPair pair2 = new PlayerPair();
    pair2.setId(1L);

    PlayerPair pair3 = new PlayerPair();
    pair3.setId(2L);

    assertEquals(pair1, pair2);
    assertNotEquals(pair1, pair3);
  }

  @Test
  void equals_shouldReturnFalseWhenIdIsNull() {
    PlayerPair pair1 = new PlayerPair();
    PlayerPair pair2 = new PlayerPair();

    assertNotEquals(pair1, pair2);
  }

  @Test
  void hashCode_shouldBeConsistent() {
    PlayerPair pair1 = new PlayerPair();
    pair1.setId(1L);

    PlayerPair pair2 = new PlayerPair();
    pair2.setId(1L);

    assertEquals(pair1.hashCode(), pair2.hashCode());
  }
}

