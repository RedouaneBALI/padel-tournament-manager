package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PlayerTest {

  @Test
  public void testPlayerAttributes() {
    Player player = new Player();
    player.setId(1L);
    player.setName("John Doe");
    player.setRanking(5);
    player.setPoints(1200);
    player.setBirthYear(1990);

    assertEquals(1L, player.getId());
    assertEquals("John Doe", player.getName());
    assertEquals(5, player.getRanking());
    assertEquals(1200, player.getPoints());
    assertEquals(1990, player.getBirthYear());
  }

  @Test
  public void testPlayerToString() {
    Player player = new Player();
    player.setName("Jane Doe");

    String expected = "Player(id=null, name=Jane Doe, ranking=null, points=null, birthYear=null)";
    assertEquals(expected, player.toString());
  }
}
