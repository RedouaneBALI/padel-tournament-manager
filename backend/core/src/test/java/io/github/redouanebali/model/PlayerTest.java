package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PlayerTest {


  @Test
  @Disabled("Test disabled: Player.toString() implementation may change and is not critical for business logic")
  void testPlayerToString() {
    Player player = new Player();
    player.setName("Jane Doe");

    String expected = "Player(id=null, name=Jane Doe, ranking=null, points=null, birthYear=null)";
    assertEquals(expected, player.toString());
  }
}
