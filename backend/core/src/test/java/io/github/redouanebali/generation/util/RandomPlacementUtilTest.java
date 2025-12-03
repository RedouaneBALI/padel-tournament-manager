package io.github.redouanebali.generation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.util.TestFixturesCore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RandomPlacementUtilTest {

  @ParameterizedTest(name = "placeRemainingTeamsRandomly with {0} teams in {1} slots")
  @CsvSource({
      "4, 8",   // Fill half the round
      "8, 8",   // Fill entire round
      "6, 16",  // Partial fill of larger round
      "0, 8"    // No teams to place
  })
  void testPlaceRemainingTeamsRandomly(int nbTeams, int drawSize) {
    // Arrange
    Round            round = TestFixturesCore.buildEmptyRound(drawSize);
    List<PlayerPair> teams = TestFixturesCore.createPlayerPairs(nbTeams);

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
    Round            round1 = TestFixturesCore.buildEmptyRound(8);
    Round            round2 = TestFixturesCore.buildEmptyRound(8);
    List<PlayerPair> teams  = TestFixturesCore.createPlayerPairs(6); // Leave some slots empty for variation

    // Act - place teams multiple times
    RandomPlacementUtil.placeRemainingTeamsRandomly(round1, teams);
    RandomPlacementUtil.placeRemainingTeamsRandomly(round2, teams);

    // Assert - with high probability, the placement should be different
    // (This test might occasionally fail due to randomness, but very rarely)
    boolean placementsDifferent = false;
    for (int i = 0; i < round1.getGames().size(); i++) {
      Game g1 = round1.getGames().get(i);
      Game g2 = round2.getGames().get(i);

      if (!Objects.equals(g1.getTeamA(), g2.getTeamA()) || !Objects.equals(g1.getTeamB(), g2.getTeamB())) {
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
    Round            round = TestFixturesCore.buildEmptyRound(drawSize);
    List<PlayerPair> teams = TestFixturesCore.createPlayerPairs(nbTeams);

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
  void testPlaceTeamsInOrder_PreservesExistingPlacements() {
    // Arrange
    Round round = TestFixturesCore.buildEmptyRound(8);

    // Place some existing teams manually
    List<PlayerPair> allTeams      = TestFixturesCore.createPlayerPairs(6);
    List<PlayerPair> existingTeams = allTeams.subList(0, 2);
    List<PlayerPair> newTeams      = allTeams.subList(2, 6);

    round.getGames().getFirst().setTeamA(existingTeams.getFirst());
    round.getGames().get(1).setTeamB(existingTeams.get(1));

    // Act
    RandomPlacementUtil.placeTeamsInOrder(round, newTeams);

    // Assert - existing teams should remain, new teams should be added
    Set<PlayerPair> allPlacedTeams = new HashSet<>();
    for (Game game : round.getGames()) {
      if (game.getTeamA() != null) {
        allPlacedTeams.add(game.getTeamA());
      }
      if (game.getTeamB() != null) {
        allPlacedTeams.add(game.getTeamB());
      }
    }

    assertEquals(6, allPlacedTeams.size(), "Should have existing teams + new teams");
    assertTrue(allPlacedTeams.containsAll(existingTeams), "Existing teams should be preserved");
    assertTrue(allPlacedTeams.containsAll(newTeams), "New teams should be placed");

    // Verify existing placements are unchanged
    assertEquals(existingTeams.getFirst(), round.getGames().getFirst().getTeamA());
    assertEquals(existingTeams.get(1), round.getGames().get(1).getTeamB());
  }

  @Test
  void testPlaceRemainingTeamsRandomly_PreservesExistingPlacements() {
    // Arrange
    Round round = TestFixturesCore.buildEmptyRound(8);

    // Place some existing teams manually
    List<PlayerPair> allTeams      = TestFixturesCore.createPlayerPairs(6);
    List<PlayerPair> existingTeams = allTeams.subList(0, 2);
    List<PlayerPair> newTeams      = allTeams.subList(2, 6);

    round.getGames().getFirst().setTeamA(existingTeams.getFirst());
    round.getGames().get(1).setTeamB(existingTeams.get(1));

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, newTeams);

    // Assert - existing teams should remain, new teams should be added
    Set<PlayerPair> allPlacedTeams = new HashSet<>();
    for (Game game : round.getGames()) {
      if (game.getTeamA() != null) {
        allPlacedTeams.add(game.getTeamA());
      }
      if (game.getTeamB() != null) {
        allPlacedTeams.add(game.getTeamB());
      }
    }

    assertEquals(6, allPlacedTeams.size(), "Should have existing teams + new teams");
    assertTrue(allPlacedTeams.containsAll(existingTeams), "Existing teams should be preserved");
    assertTrue(allPlacedTeams.containsAll(newTeams), "New teams should be placed");

    // Verify existing placements are unchanged
    assertEquals(existingTeams.getFirst(), round.getGames().getFirst().getTeamA());
    assertEquals(existingTeams.get(1), round.getGames().get(1).getTeamB());
  }

  @Test
  void testPlaceRemainingTeamsRandomly_HandlesNullRound() {
    // Act & Assert
    RandomPlacementUtil.placeRemainingTeamsRandomly(null, TestFixturesCore.createPlayerPairs(4));
    // Should not throw exception
    assertTrue(true, "Should handle null teams without throwing exception");
  }

  @Test
  void testPlaceRemainingTeamsRandomly_HandlesNullTeams() {
    Round round = TestFixturesCore.buildEmptyRound(8);
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, null);
    // Should not throw exception
    assertTrue(true, "Should handle null teams without throwing exception");
  }

  @Test
  void testPlaceTeamsInOrder_HandlesOverflow() {
    // Arrange
    Round            round = TestFixturesCore.buildEmptyRound(4); // Only 4 slots total
    List<PlayerPair> teams = TestFixturesCore.createPlayerPairs(10); // More teams than slots

    // Act
    RandomPlacementUtil.placeTeamsInOrder(round, teams);

    // Assert - only as many teams as there are slots should be placed
    Set<PlayerPair> placedTeams = new HashSet<>();
    for (Game game : round.getGames()) {
      if (game.getTeamA() != null) {
        placedTeams.add(game.getTeamA());
      }
      if (game.getTeamB() != null) {
        placedTeams.add(game.getTeamB());
      }
    }

    assertEquals(4, placedTeams.size(), "Should place exactly 4 teams (all available slots)");

    // First 4 teams should be placed in order
    List<PlayerPair> expectedFirstFour = teams.subList(0, 4);
    assertTrue(placedTeams.containsAll(expectedFirstFour), "First 4 teams should be placed");
  }

  @Test
  void testPlaceRemainingTeamsRandomly_HandlesOverflow() {
    // Arrange
    Round            round = TestFixturesCore.buildEmptyRound(4); // Only 4 slots total
    List<PlayerPair> teams = TestFixturesCore.createPlayerPairs(10); // More teams than slots

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, teams);

    // Assert - only as many teams as there are slots should be placed
    Set<PlayerPair> placedTeams = new HashSet<>();
    for (Game game : round.getGames()) {
      if (game.getTeamA() != null) {
        placedTeams.add(game.getTeamA());
      }
      if (game.getTeamB() != null) {
        placedTeams.add(game.getTeamB());
      }
    }

    assertEquals(4, placedTeams.size(), "Should place exactly 4 teams (all available slots)");
    assertTrue(teams.containsAll(placedTeams), "All placed teams should be from the provided list");
  }

  @Test
  void testPlaceTeamsInOrder_HandlesNullRound() {
    // Act & Assert
    RandomPlacementUtil.placeTeamsInOrder(null, TestFixturesCore.createPlayerPairs(4));
    // Should not throw exception
    assertTrue(true, "Should handle null teams without throwing exception");
  }

  @Test
  void testPlaceTeamsInOrder_HandlesNullTeams() {
    Round round = TestFixturesCore.buildEmptyRound(8);
    RandomPlacementUtil.placeTeamsInOrder(round, null);
    // Should not throw exception
    assertTrue(true, "Should handle null teams without throwing exception");
  }

  // -------------------- Pool-based tests --------------------

  @ParameterizedTest(name = "placeRemainingTeamsRandomly in pools: {0} teams, {1} pools")
  @CsvSource({
      "9, 3", // Exactly divisible
      "10, 3", // One extra team
      "8, 3"  // One less team per pool
  })
  void testPlaceRemainingTeamsRandomly_InPools(int totalTeams, int nbPools) {
    // Arrange
    Round            round = TestFixturesCore.buildEmptyPoolRound(nbPools, totalTeams / nbPools);
    List<PlayerPair> teams = TestFixturesCore.createPlayerPairs(totalTeams);

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, teams);

    // Assert - verify all teams are placed across all pools
    Set<PlayerPair> allPlacedTeams = new HashSet<>();
    for (Pool pool : round.getPools()) {
      allPlacedTeams.addAll(pool.getPairs());
    }

    assertEquals(totalTeams, allPlacedTeams.size(), "All teams should be placed");
    assertTrue(allPlacedTeams.containsAll(teams), "All provided teams should be placed");

    // Verify distribution is reasonably balanced (no pool should be completely empty unless no teams)
    if (totalTeams > 0) {
      boolean hasNonEmptyPool = round.getPools().stream().anyMatch(pool -> !pool.getPairs().isEmpty());
      assertTrue(hasNonEmptyPool, "At least one pool should have teams when teams are provided");
    }
  }

  @Test
  void testPlaceRemainingTeamsRandomly_InPools_ExactlyDivisible() {
    // Arrange
    Round            round = TestFixturesCore.buildEmptyPoolRound(3, 3);
    List<PlayerPair> teams = TestFixturesCore.createPlayerPairs(9); // Exactly divisible by 3

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, teams);

    // Assert - each pool should have exactly 3 teams
    for (Pool pool : round.getPools()) {
      assertEquals(3, pool.getPairs().size(), "Each pool should have exactly 3 teams");
    }

    // Verify all teams are placed
    Set<PlayerPair> allPlacedTeams = new HashSet<>();
    for (Pool pool : round.getPools()) {
      allPlacedTeams.addAll(pool.getPairs());
    }
    assertEquals(9, allPlacedTeams.size(), "All 9 teams should be placed");
  }

  @Test
  void testPlaceRemainingTeamsRandomly_InPools_PreservesExistingTeams() {
    // Arrange
    Round round = TestFixturesCore.buildEmptyPoolRound(2, 3);

    // Add some existing teams to pools
    List<PlayerPair> allTeams      = TestFixturesCore.createPlayerPairs(6);
    List<PlayerPair> existingTeams = allTeams.subList(0, 2); // First 2 teams
    List<PlayerPair> newTeams      = allTeams.subList(2, 6); // Teams 3-6

    round.getPools().getFirst().addPair(existingTeams.getFirst());
    round.getPools().get(1).addPair(existingTeams.get(1));

    // Act
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, newTeams);

    // Assert - existing teams should remain, new teams should be added
    Set<PlayerPair> allPlacedTeams = new HashSet<>();
    for (Pool pool : round.getPools()) {
      allPlacedTeams.addAll(pool.getPairs());
    }

    assertEquals(6, allPlacedTeams.size(), "Should have existing teams + new teams");
    assertTrue(allPlacedTeams.containsAll(existingTeams), "Existing teams should be preserved");
    assertTrue(allPlacedTeams.containsAll(newTeams), "New teams should be placed");

    // Verify existing teams are still in their original pools
    assertTrue(round.getPools().getFirst().getPairs().contains(existingTeams.getFirst()));
    assertTrue(round.getPools().get(1).getPairs().contains(existingTeams.get(1)));
  }

  @Test
  void testPlaceTeamsInOrder_InPools() {
    // Arrange
    Round            round = TestFixturesCore.buildEmptyPoolRound(2, 2);
    List<PlayerPair> teams = TestFixturesCore.createPlayerPairs(4);

    // Act
    RandomPlacementUtil.placeTeamsInOrder(round, teams);

    // Assert - teams should be distributed across pools in order
    assertEquals(2, round.getPools().getFirst().getPairs().size(), "First pool should have 2 teams");
    assertEquals(2, round.getPools().get(1).getPairs().size(), "Second pool should have 2 teams");

    // Verify all teams are placed
    Set<PlayerPair> allPlacedTeams = new HashSet<>();
    for (Pool pool : round.getPools()) {
      allPlacedTeams.addAll(pool.getPairs());
    }
    assertEquals(4, allPlacedTeams.size(), "All teams should be placed");
    assertTrue(allPlacedTeams.containsAll(teams), "All provided teams should be placed");
  }

  @Test
  void testPlaceRemainingTeamsRandomly_InPools_HandlesEmptyPools() {
    // Arrange
    Round            round = TestFixturesCore.buildEmptyPoolRound(2, 2);
    List<PlayerPair> teams = TestFixturesCore.createPlayerPairs(2); // Fewer teams than pools

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

}
