package io.github.redouanebali.generation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
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

  @Test
  void testPlaceRemainingTeamsRandomly_WithMoreTeamsThanSlots() {
    // Arrange
    Round            round = TestFixtures.buildEmptyRound(8);
    List<PlayerPair> teams = createTestPlayerPairs(10); // More teams than slots

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, teams);

    // Assert - verify only as many teams as slots are placed
    Set<PlayerPair> placedTeams = new HashSet<>();
    for (Game game : round.getGames()) {
      if (game.getTeamA() != null) {
        placedTeams.add(game.getTeamA());
      }
      if (game.getTeamB() != null) {
        placedTeams.add(game.getTeamB());
      }
    }

    assertEquals(8, placedTeams.size(), "Should place only as many teams as there are slots");
    assertTrue(teams.containsAll(placedTeams), "All placed teams should be from the input list");
  }

  // ========== NEW TESTS FOR POOLS (GROUP PHASE) ==========

  @ParameterizedTest(name = "placeRemainingTeamsRandomly in pools: {0} teams, {1} pools")
  @CsvSource({
      "12, 4",  // 12 teams in 4 pools (3 per pool)
      "16, 4",  // 16 teams in 4 pools (4 per pool)
      "8, 2",   // 8 teams in 2 pools (4 per pool)
      "10, 5",  // 10 teams in 5 pools (2 per pool)
      "0, 4"    // No teams to place
  })
  void testPlaceRemainingTeamsRandomly_InPools(int totalTeams, int nbPools) {
    // Arrange
    Round            round = createRoundWithPools(nbPools);
    List<PlayerPair> teams = createTestPlayerPairs(totalTeams);

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, teams);

    // Assert - verify all teams are placed
    Set<PlayerPair> placedTeams = new HashSet<>();
    for (Pool pool : round.getPools()) {
      placedTeams.addAll(pool.getPairs());
    }

    assertEquals(totalTeams, placedTeams.size(), "All teams should be placed exactly once");
    assertTrue(placedTeams.containsAll(teams), "All provided teams should be placed");

    // Verify teams are distributed across pools
    if (totalTeams > 0 && nbPools > 1) {
      int minTeamsPerPool = totalTeams / nbPools;
      int maxTeamsPerPool = minTeamsPerPool + (totalTeams % nbPools > 0 ? 1 : 0);

      for (Pool pool : round.getPools()) {
        int teamsInPool = pool.getPairs().size();
        assertTrue(teamsInPool >= minTeamsPerPool && teamsInPool <= maxTeamsPerPool,
                   "Pool should have between " + minTeamsPerPool + " and " + maxTeamsPerPool + " teams, but had " + teamsInPool);
      }
    }
  }

  @Test
  void testPlaceRemainingTeamsRandomly_InPools_RoundRobinDistribution() {
    // Arrange
    Round            round = createRoundWithPools(3);
    List<PlayerPair> teams = createTestPlayerPairs(9); // Exactly divisible by 3

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, teams);

    // Assert - verify even distribution (3 teams per pool)
    for (Pool pool : round.getPools()) {
      assertEquals(3, pool.getPairs().size(), "Each pool should have exactly 3 teams");
    }

    // Verify no duplicates across pools
    Set<PlayerPair> allPlacedTeams = new HashSet<>();
    for (Pool pool : round.getPools()) {
      for (PlayerPair pair : pool.getPairs()) {
        assertTrue(allPlacedTeams.add(pair), "Team should not appear in multiple pools: " + pair);
      }
    }
  }

  @Test
  void testPlaceRemainingTeamsRandomly_InPools_WithPreExistingTeams() {
    // Arrange
    Round            round         = createRoundWithPools(2);
    List<PlayerPair> existingTeams = createTestPlayerPairs(2);
    List<PlayerPair> newTeams      = createTestPlayerPairs(4, 3);

    // Pre-place some teams
    round.getPools().get(0).addPair(existingTeams.get(0));
    round.getPools().get(1).addPair(existingTeams.get(1));

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, newTeams);

    // Assert - verify existing teams are not disturbed
    assertTrue(round.getPools().get(0).getPairs().contains(existingTeams.get(0)),
               "Existing team in pool 0 should not be disturbed");
    assertTrue(round.getPools().get(1).getPairs().contains(existingTeams.get(1)),
               "Existing team in pool 1 should not be disturbed");

    // Verify new teams are placed
    Set<PlayerPair> allPlacedTeams = new HashSet<>();
    for (Pool pool : round.getPools()) {
      allPlacedTeams.addAll(pool.getPairs());
    }

    assertTrue(allPlacedTeams.containsAll(newTeams), "All new teams should be placed");
    assertEquals(6, allPlacedTeams.size(), "Total of 6 teams should be placed (2 existing + 4 new)");
  }

  @Test
  void testPlaceRemainingTeamsRandomly_InPools_AvoidsDuplicates() {
    // Arrange
    Round            round = createRoundWithPools(2);
    List<PlayerPair> teams = createTestPlayerPairs(4);

    // Pre-place one team in first pool
    round.getPools().get(0).addPair(teams.get(0));

    // Act - try to place all teams (including the already placed one)
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, teams);

    // Assert - verify no duplicates
    Set<PlayerPair> allPlacedTeams = new HashSet<>();
    for (Pool pool : round.getPools()) {
      for (PlayerPair pair : pool.getPairs()) {
        assertTrue(allPlacedTeams.add(pair), "Team should not appear in multiple pools: " + pair);
      }
    }

    // Should have exactly 4 teams placed (no duplicates)
    assertEquals(4, allPlacedTeams.size(), "Should have exactly 4 teams placed");
    assertTrue(allPlacedTeams.containsAll(teams), "All teams should be placed");
  }

  @Test
  void testPlaceRemainingTeamsRandomly_InPools_HandlesEmptyPools() {
    // Arrange
    Round            round = createRoundWithPools(3);
    List<PlayerPair> teams = createTestPlayerPairs(2); // Fewer teams than pools

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, teams);

    // Assert - verify teams are placed and some pools may remain empty
    Set<PlayerPair> allPlacedTeams = new HashSet<>();
    for (Pool pool : round.getPools()) {
      allPlacedTeams.addAll(pool.getPairs());
    }

    assertEquals(2, allPlacedTeams.size(), "All teams should be placed");
    assertTrue(allPlacedTeams.containsAll(teams), "All provided teams should be placed");

    // At least one pool should have teams
    boolean hasNonEmptyPool = round.getPools().stream().anyMatch(pool -> !pool.getPairs().isEmpty());
    assertTrue(hasNonEmptyPool, "At least one pool should have teams");
  }

  // Helper method to create a round with pools (for group phase testing)
  private Round createRoundWithPools(int nbPools) {
    Round round = new Round();
    for (int i = 0; i < nbPools; i++) {
      Pool pool = new Pool();
      pool.setName("Pool " + (char) ('A' + i));
      round.addPool(pool);
    }
    return round;
  }

  // Helper method to create test player pairs
  @Deprecated
  private List<PlayerPair> createTestPlayerPairs(int count) {
    return createTestPlayerPairs(count, 1);
  }

  @Deprecated
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
