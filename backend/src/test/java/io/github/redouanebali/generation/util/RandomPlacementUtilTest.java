package io.github.redouanebali.generation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class RandomPlacementUtilTest {

  @ParameterizedTest(name = "placeRemainingTeamsRandomly with {0} teams in {1} slots")
  @CsvSource({
      "4, 8",   // Fill half the round
      "8, 8",   // Fill entire round
      "6, 16",  // Partial fill of larger round
      "0, 8"    // No teams to place
  })
  void testPlaceRemainingTeamsRandomly(int nbTeams, int drawSize) {
    // Arrange
    Round            round = TestFixtures.buildEmptyRound(drawSize);
    List<PlayerPair> teams = createTestPlayerPairs(nbTeams);

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, teams);

    // Assert - verify all teams are placed
    Set<PlayerPair> placedTeams = new HashSet<>();
    for (Game game : round.getGames()) {
      if (game.getTeamA() != null) {
        placedTeams.add(game.getTeamA());
      }
      if (game.getTeamB() != null) {
        placedTeams.add(game.getTeamB());
      }
    }

    assertEquals(nbTeams, placedTeams.size(), "All teams should be placed exactly once");
    assertTrue(placedTeams.containsAll(teams), "All provided teams should be placed");

    // Verify no team is placed more than once
    long totalPlacements = round.getGames().stream()
                                .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                .filter(t -> t != null && teams.contains(t))
                                .count();
    assertEquals(nbTeams, totalPlacements, "No team should be placed more than once");
  }

  @Test
  void testPlaceRemainingTeamsRandomly_IsActuallyRandom() {
    // Arrange
    Round            round1 = TestFixtures.buildEmptyRound(8);
    Round            round2 = TestFixtures.buildEmptyRound(8);
    List<PlayerPair> teams  = createTestPlayerPairs(6); // Leave some slots empty for variation

    // Act - place teams multiple times
    RandomPlacementUtil.placeRemainingTeamsRandomly(round1, teams);
    RandomPlacementUtil.placeRemainingTeamsRandomly(round2, teams);

    // Assert - with high probability, the placement should be different
    // (This test might occasionally fail due to randomness, but very rarely)
    boolean placementsDifferent = false;
    for (int i = 0; i < round1.getGames().size(); i++) {
      Game g1 = round1.getGames().get(i);
      Game g2 = round2.getGames().get(i);

      if (!equals(g1.getTeamA(), g2.getTeamA()) || !equals(g1.getTeamB(), g2.getTeamB())) {
        placementsDifferent = true;
        break;
      }
    }

    // Note: This test has a small chance of false failure if random placement happens to be identical
    // In practice, with 6 teams in 8 slots, the probability of identical placement is very low
    assertTrue(placementsDifferent,
               "Random placement should produce different results (may rarely fail due to randomness)");
  }

  @ParameterizedTest(name = "placeTeamsInOrder with {0} teams in {1} slots")
  @CsvSource({
      "4, 8",   // Fill half the round
      "8, 8",   // Fill entire round
      "6, 16",  // Partial fill of larger round
      "0, 8"    // No teams to place
  })
  void testPlaceTeamsInOrder(int nbTeams, int drawSize) {
    // Arrange
    Round            round = TestFixtures.buildEmptyRound(drawSize);
    List<PlayerPair> teams = createTestPlayerPairs(nbTeams);

    // Act
    RandomPlacementUtil.placeTeamsInOrder(round, teams);

    // Assert - verify teams are placed in order
    List<PlayerPair> placedInOrder = new ArrayList<>();
    for (Game game : round.getGames()) {
      if (game.getTeamA() != null) {
        placedInOrder.add(game.getTeamA());
      }
      if (game.getTeamB() != null) {
        placedInOrder.add(game.getTeamB());
      }
    }

    assertEquals(nbTeams, placedInOrder.size(), "All teams should be placed");

    // Verify order is preserved
    for (int i = 0; i < nbTeams; i++) {
      assertEquals(teams.get(i), placedInOrder.get(i),
                   "Team at index " + i + " should be placed in order");
    }
  }

  @Test
  void testPlaceTeamsInOrder_DeterministicPlacement() {
    // Arrange
    Round            round1 = TestFixtures.buildEmptyRound(8);
    Round            round2 = TestFixtures.buildEmptyRound(8);
    List<PlayerPair> teams  = createTestPlayerPairs(6);

    // Act - place teams in order multiple times
    RandomPlacementUtil.placeTeamsInOrder(round1, teams);
    RandomPlacementUtil.placeTeamsInOrder(round2, teams);

    // Assert - placement should be identical (deterministic)
    for (int i = 0; i < round1.getGames().size(); i++) {
      Game g1 = round1.getGames().get(i);
      Game g2 = round2.getGames().get(i);

      assertEquals(g1.getTeamA(), g2.getTeamA(),
                   "TeamA should be identical in game " + i);
      assertEquals(g1.getTeamB(), g2.getTeamB(),
                   "TeamB should be identical in game " + i);
    }
  }

  @Test
  void testPlaceRemainingTeamsRandomly_HandlesNullInputs() {
    // Test null round
    RandomPlacementUtil.placeRemainingTeamsRandomly(null, createTestPlayerPairs(4));
    // Should not throw

    // Test null games
    Round round = new Round();
    round.replaceGames(null);
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, createTestPlayerPairs(4));
    // Should not throw

    // Test null teams
    Round validRound = TestFixtures.buildEmptyRound(8);
    RandomPlacementUtil.placeRemainingTeamsRandomly(validRound, null);
    // Should not throw and should not place anything
    for (Game g : validRound.getGames()) {
      assertNull(g.getTeamA());
      assertNull(g.getTeamB());
    }

    // Test empty teams list
    RandomPlacementUtil.placeRemainingTeamsRandomly(validRound, new ArrayList<>());
    // Should not throw and should not place anything
    for (Game g : validRound.getGames()) {
      assertNull(g.getTeamA());
      assertNull(g.getTeamB());
    }
  }

  @Test
  void testPlaceTeamsInOrder_HandlesNullInputs() {
    // Test null round
    RandomPlacementUtil.placeTeamsInOrder(null, createTestPlayerPairs(4));
    // Should not throw

    // Test null games
    Round round = new Round();
    round.replaceGames(null);
    RandomPlacementUtil.placeTeamsInOrder(round, createTestPlayerPairs(4));
    // Should not throw

    // Test null teams
    Round validRound = TestFixtures.buildEmptyRound(8);
    RandomPlacementUtil.placeTeamsInOrder(validRound, null);
    // Should not throw and should not place anything
    for (Game g : validRound.getGames()) {
      assertNull(g.getTeamA());
      assertNull(g.getTeamB());
    }

    // Test empty teams list
    RandomPlacementUtil.placeTeamsInOrder(validRound, new ArrayList<>());
    // Should not throw and should not place anything
    for (Game g : validRound.getGames()) {
      assertNull(g.getTeamA());
      assertNull(g.getTeamB());
    }
  }

  @Test
  void testPlaceRemainingTeamsRandomly_WithPartiallyFilledRound() {
    // Arrange - pre-fill some slots
    Round            round         = TestFixtures.buildEmptyRound(8);
    List<PlayerPair> existingTeams = createTestPlayerPairs(2);
    round.getGames().get(0).setTeamA(existingTeams.get(0));
    round.getGames().get(1).setTeamB(existingTeams.get(1));

    List<PlayerPair> newTeams = createTestPlayerPairs(4, 3); // Start from index 3

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, newTeams);

    // Assert - verify new teams are placed without disturbing existing ones
    assertEquals(existingTeams.get(0), round.getGames().get(0).getTeamA(),
                 "Existing team should not be disturbed");
    assertEquals(existingTeams.get(1), round.getGames().get(1).getTeamB(),
                 "Existing team should not be disturbed");

    // Count placed new teams
    Set<PlayerPair> placedNewTeams = new HashSet<>();
    for (Game game : round.getGames()) {
      if (game.getTeamA() != null && newTeams.contains(game.getTeamA())) {
        placedNewTeams.add(game.getTeamA());
      }
      if (game.getTeamB() != null && newTeams.contains(game.getTeamB())) {
        placedNewTeams.add(game.getTeamB());
      }
    }

    assertEquals(4, placedNewTeams.size(), "All new teams should be placed");
  }

  @Test
  void testPlaceTeamsInOrder_WithPartiallyFilledRound() {
    // Arrange - pre-fill some slots
    Round            round         = TestFixtures.buildEmptyRound(8);
    List<PlayerPair> existingTeams = createTestPlayerPairs(2);
    round.getGames().get(0).setTeamA(existingTeams.get(0));
    round.getGames().get(1).setTeamB(existingTeams.get(1));

    List<PlayerPair> newTeams = createTestPlayerPairs(4, 3); // Start from index 3

    // Act
    RandomPlacementUtil.placeTeamsInOrder(round, newTeams);

    // Assert - verify new teams are placed in order without disturbing existing ones
    assertEquals(existingTeams.get(0), round.getGames().get(0).getTeamA(),
                 "Existing team should not be disturbed");
    assertEquals(existingTeams.get(1), round.getGames().get(1).getTeamB(),
                 "Existing team should not be disturbed");

    // Verify new teams are placed in available slots in order
    List<PlayerPair> placedNewTeams = new ArrayList<>();
    for (Game game : round.getGames()) {
      if (game.getTeamA() != null && newTeams.contains(game.getTeamA())) {
        placedNewTeams.add(game.getTeamA());
      }
      if (game.getTeamB() != null && newTeams.contains(game.getTeamB())) {
        placedNewTeams.add(game.getTeamB());
      }
    }

    assertEquals(4, placedNewTeams.size(), "All new teams should be placed");
    // Verify order is maintained
    for (int i = 0; i < 4; i++) {
      assertEquals(newTeams.get(i), placedNewTeams.get(i),
                   "New teams should be placed in order");
    }
  }

  @Test
  void testPlaceRemainingTeamsRandomly_MoreTeamsThanSlots() {
    // Arrange
    Round            round = TestFixtures.buildEmptyRound(8); // Only 8 slots
    List<PlayerPair> teams = createTestPlayerPairs(10); // More teams than slots

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, teams);

    // Assert - only 8 teams should be placed (limited by available slots)
    long placedCount = round.getGames().stream()
                            .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                            .filter(t -> t != null)
                            .count();
    assertEquals(8, placedCount, "Should place only as many teams as there are slots");

    // Verify all placed teams are from the original list
    Set<PlayerPair> placedTeams = new HashSet<>();
    for (Game game : round.getGames()) {
      if (game.getTeamA() != null) {
        placedTeams.add(game.getTeamA());
        assertTrue(teams.contains(game.getTeamA()), "Placed team should be from original list");
      }
      if (game.getTeamB() != null) {
        placedTeams.add(game.getTeamB());
        assertTrue(teams.contains(game.getTeamB()), "Placed team should be from original list");
      }
    }
  }

  // Helper method to create test player pairs
  private List<PlayerPair> createTestPlayerPairs(int count) {
    return createTestPlayerPairs(count, 1);
  }

  private List<PlayerPair> createTestPlayerPairs(int count, int startIndex) {
    List<PlayerPair> pairs = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      int actualIndex = startIndex + i;

      Player player1 = new Player();
      player1.setName("Player" + (actualIndex * 2 - 1));

      Player player2 = new Player();
      player2.setName("Player" + (actualIndex * 2));

      PlayerPair pair = new PlayerPair();
      pair.setPlayer1(player1);
      pair.setPlayer2(player2);
      pair.setSeed(0); // Non-seeded for these tests

      pairs.add(pair);
    }
    return pairs;
  }

  // Helper method to compare PlayerPair objects (handles null)
  private boolean equals(PlayerPair p1, PlayerPair p2) {
    if (p1 == null && p2 == null) {
      return true;
    }
    if (p1 == null || p2 == null) {
      return false;
    }
    return p1.equals(p2);
  }
}
