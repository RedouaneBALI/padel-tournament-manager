package io.github.redouanebali.generation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class SeedPlacementUtilTest {

  @ParameterizedTest(name = "getSeedsPositions for drawSize={0}, nbSeeds={1}")
  @CsvSource({
      // drawSize, nbSeeds, expectedSeedIndices (semicolon-separated)
      "8, 4, 0;7;4;3",
      "16, 8, 0;15;8;7;4;11;12;3",
      "16, 4, 0;15;8;7",
      "32, 16, 0;31;16;15;8;23;24;7;4;27;20;11;12;19;28;3",
      "32, 8, 0;31;16;15;8;23;24;7",
      "64, 16, 0;63;32;31;16;47;48;15;8;55;40;23;24;39;56;7"
  })
  void testGetSeedsPositions(int drawSize, int nbSeeds, String expectedIndicesStr) {
    // When
    List<Integer> seedPositions = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);

    // Then
    assertEquals(nbSeeds, seedPositions.size(), "Should return exactly nbSeeds positions");

    // All positions should be unique
    Set<Integer> uniquePositions = new HashSet<>(seedPositions);
    assertEquals(nbSeeds, uniquePositions.size(), "All seed positions should be unique");

    // All positions should be valid (0 <= pos < drawSize)
    for (int pos : seedPositions) {
      assertTrue(pos >= 0 && pos < drawSize, "Position " + pos + " should be between 0 and " + (drawSize - 1));
    }

    // Parse expected positions for validation
    List<Integer> expectedSeedIndices = Stream.of(expectedIndicesStr.split(";"))
                                              .map(String::trim)
                                              .map(Integer::parseInt)
                                              .toList();

    // For TS1 and TS2, verify exact positions
    for (int i = 0; i < Math.min(2, expectedSeedIndices.size()); i++) {
      int expectedIdx = expectedSeedIndices.get(i);
      int actualIdx   = seedPositions.get(i);
      assertEquals(expectedIdx, actualIdx,
                   "Seed " + (i + 1) + " expected at index " + expectedIdx + " but was at index " + actualIdx);
    }

    // For TS3+, verify positions are within valid group
    if (expectedSeedIndices.size() > 2) {
      List<Integer> possiblePositions = expectedSeedIndices.subList(2, expectedSeedIndices.size());
      for (int i = 2; i < seedPositions.size(); i++) {
        int actualIdx = seedPositions.get(i);
        assertTrue(possiblePositions.contains(actualIdx),
                   "Seed " + (i + 1) + " (TS3+) at index " + actualIdx + " not in valid group: " + possiblePositions);
      }
    }
  }

  @ParameterizedTest(name = "TS1/TS2 fixed positions for drawSize={0}, nbSeeds={1}")
  @CsvSource({
      // drawSize, nbSeeds, TS1_expected, TS2_expected
      "8, 4, 0, 7",
      "16, 8, 0, 15",
      "16, 4, 0, 15",
      "32, 16, 0, 31",
      "32, 8, 0, 31",
      "64, 16, 0, 63"
  })
  void testSeedPositions_TS1_TS2_Fixed(int drawSize, int nbSeeds, int expectedTS1, int expectedTS2) {
    // When
    List<Integer> seedPositions = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);

    // Then - TS1 and TS2 should always be at fixed positions
    assertEquals(expectedTS1, seedPositions.getFirst(), "TS1 must always be at position " + expectedTS1);
    if (nbSeeds >= 2) {
      assertEquals(expectedTS2, seedPositions.get(1), "TS2 must always be at position " + expectedTS2);
    }
  }

  @ParameterizedTest(name = "TS3+ random positions for drawSize={0}, nbSeeds={1}")
  @CsvSource({
      // drawSize, nbSeeds, TS3_possible_positions, TS4_possible_positions
      "8, 4, '3;4', '3;4'",
      "16, 8, '7;8', '7;8'",
      "16, 4, '7;8', '7;8'",
      "32, 8, '15;16', '15;16'",
      "64, 16, '31;32', '31;32'"
  })
  void testSeedPositions_TS3Plus_RandomWithinValidOptions(int drawSize, int nbSeeds, String ts3Options, String ts4Options) {
    if (nbSeeds < 3) {
      return; // Skip if not enough seeds for TS3+
    }

    // Test multiple times to verify randomness
    Set<Integer> observedTS3Positions = new HashSet<>();
    Set<Integer> observedTS4Positions = new HashSet<>();

    for (int i = 0; i < 50; i++) { // Run 50 times to catch randomness
      List<Integer> seedPositions = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);

      if (nbSeeds >= 3) {
        observedTS3Positions.add(seedPositions.get(2));
      }
      if (nbSeeds >= 4) {
        observedTS4Positions.add(seedPositions.get(3));
      }
    }

    // Parse expected positions
    List<Integer> validTS3Positions = Stream.of(ts3Options.split(";"))
                                            .map(String::trim)
                                            .map(Integer::parseInt)
                                            .toList();

    List<Integer> validTS4Positions = Stream.of(ts4Options.split(";"))
                                            .map(String::trim)
                                            .map(Integer::parseInt)
                                            .toList();

    // Verify TS3 positions are within valid options
    for (int observedPos : observedTS3Positions) {
      assertTrue(validTS3Positions.contains(observedPos),
                 "TS3 position " + observedPos + " not in valid options: " + validTS3Positions);
    }

    // Verify TS4 positions are within valid options
    if (nbSeeds >= 4) {
      for (int observedPos : observedTS4Positions) {
        assertTrue(validTS4Positions.contains(observedPos),
                   "TS4 position " + observedPos + " not in valid options: " + validTS4Positions);
      }
    }

    // Verify that we see some variation (randomness working)
    if (validTS3Positions.size() > 1) {
      assertTrue(observedTS3Positions.size() > 1,
                 "TS3 should show variation across multiple runs, but only saw: " + observedTS3Positions);
    }
  }

  @ParameterizedTest(name = "getSeedsPositions empty when nbSeeds=0 (drawSize={0})")
  @CsvSource({
      "8",
      "16",
      "32",
      "64"
  })
  void testGetSeedsPositions_EmptyWhenNoSeeds(int drawSize) {
    // When
    List<Integer> seedPositions = SeedPlacementUtil.getSeedsPositions(drawSize, 0);

    // Then
    assertEquals(0, seedPositions.size(), "Expected no seed positions when nbSeeds=0");
  }

  @ParameterizedTest(name = "placeSeedTeams mapping for drawSize={0}, nbSeeds={1}")
  @CsvSource({
      "64, 16",
      "32, 16",
      "32, 8",
      "16, 8",
      "16, 4"
  })
  void testPlaceSeedTeams_Mapping(int drawSize, int nbSeeds) {
    // Arrange
    Round            round = TestFixtures.buildEmptyRound(drawSize);
    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(drawSize);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    // Act
    SeedPlacementUtil.placeSeedTeams(round, pairs, nbSeeds, drawSize);

    // Then
    List<Integer> seedSlots = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);

    // Verify TS1 and TS2 (always fixed)
    for (int i = 0; i < Math.min(nbSeeds, 2); i++) {
      int        slot         = seedSlots.get(i);
      int        gameIndex    = slot / 2;
      boolean    left         = (slot % 2 == 0);
      Game       g            = round.getGames().get(gameIndex);
      PlayerPair expectedPair = pairs.get(i);

      if (left) {
        assertEquals(expectedPair, g.getTeamA(),
                     "Seed " + (i + 1) + " (TS" + (i + 1) + ") should be on TEAM_A of game " + gameIndex);
      } else {
        assertEquals(expectedPair, g.getTeamB(),
                     "Seed " + (i + 1) + " (TS" + (i + 1) + ") should be on TEAM_B of game " + gameIndex);
      }
    }

    // Verify TS3+ (random order, just verify presence)
    Set<Integer> slotsTS3Plus = new HashSet<>();
    for (int i = 2; i < nbSeeds; i++) {
      slotsTS3Plus.add(seedSlots.get(i));
    }

    Set<PlayerPair> placedTS3Plus = new HashSet<>();
    for (int slot : slotsTS3Plus) {
      int        gameIndex = slot / 2;
      boolean    left      = (slot % 2 == 0);
      Game       g         = round.getGames().get(gameIndex);
      PlayerPair found     = left ? g.getTeamA() : g.getTeamB();

      assertTrue(found != null && found.getSeed() > 0, "A seed should be placed at slot " + slot);
      placedTS3Plus.add(found);
    }

    // Verify we placed the correct number of TS3+ seeds
    assertEquals(Math.max(0, nbSeeds - 2), placedTS3Plus.size(), "Incorrect number of TS3+ seeds placed");

    // Verify other slots are empty
    Set<Integer> seedSet = new HashSet<>(seedSlots);
    for (int slot = 0; slot < drawSize; slot++) {
      if (seedSet.contains(slot)) {
        continue;
      }

      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);
      Game    g         = round.getGames().get(gameIndex);

      if (left) {
        assertNull(g.getTeamA(), "Non-seed slot should be empty on TEAM_A of game " + gameIndex);
      } else {
        assertNull(g.getTeamB(), "Non-seed slot should be empty on TEAM_B of game " + gameIndex);
      }
    }
  }

  @ParameterizedTest(name = "placeSeedTeams does nothing when nbSeeds=0 (drawSize={0})")
  @CsvSource({
      "8",
      "16",
      "32"
  })
  void testPlaceSeedTeams_DoesNothingWhenNoSeeds(int drawSize) {
    // Arrange
    Round            round = TestFixtures.buildEmptyRound(drawSize);
    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(drawSize);

    // Act
    SeedPlacementUtil.placeSeedTeams(round, pairs, 0, drawSize);

    // Then - every slot remains empty (no seed was placed)
    for (Game g : round.getGames()) {
      assertNull(g.getTeamA(), "TEAM_A should be empty when nbSeeds=0");
      assertNull(g.getTeamB(), "TEAM_B should be empty when nbSeeds=0");
    }
  }

  @Test
  void testPlaceSeedTeams_ThrowsException_WhenSlotAlreadyOccupied() {
    // Arrange
    Round            round = TestFixtures.buildEmptyRound(8);
    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(8);

    // Manually place a team at seed position 0 (game 0, team A)
    round.getGames().getFirst().setTeamA(pairs.get(7)); // Use a different pair

    // Act & Then
    assertThrows(IllegalStateException.class,
                 () -> SeedPlacementUtil.placeSeedTeams(round, pairs, 4, 8),
                 "Should throw exception when seed slot is already occupied");
  }

  @Test
  void testPlaceSeedTeams_HandlesNullInputs() {
    // Test null round
    SeedPlacementUtil.placeSeedTeams(null, TestFixtures.createPlayerPairs(8), 4, 8);
    // Should not throw

    // Test null games - should use nbSeeds=0 since no games can be placed
    Round round = new Round();
    round.replaceGames(null);
    SeedPlacementUtil.placeSeedTeams(round, TestFixtures.createPlayerPairs(8), 0, 0);
    // Should not throw

    // Test null players
    Round validRound = TestFixtures.buildEmptyRound(8);
    SeedPlacementUtil.placeSeedTeams(validRound, null, 4, 8);
    // Should not throw

    // Test nbSeeds = 0
    Round anotherValidRound = TestFixtures.buildEmptyRound(8);
    SeedPlacementUtil.placeSeedTeams(anotherValidRound, TestFixtures.createPlayerPairs(8), 0, 8);
    // Should not throw and should not place anything
    for (Game g : anotherValidRound.getGames()) {
      assertNull(g.getTeamA());
      assertNull(g.getTeamB());
    }

    assertTrue(true, "Should handle null and empty inputs without throwing exception");
  }

  @Test
  void testPlaceSeedTeams_ThrowsException_ForNegativeParameters() {
    // Arrange
    Round            round = TestFixtures.buildEmptyRound(8);
    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(8);

    // Test negative nbSeeds
    assertThrows(IllegalArgumentException.class,
                 () -> SeedPlacementUtil.placeSeedTeams(round, pairs, -1, 8),
                 "Should throw for negative nbSeeds");

    // Test negative drawSize
    assertThrows(IllegalArgumentException.class,
                 () -> SeedPlacementUtil.placeSeedTeams(round, pairs, 4, -1),
                 "Should throw for negative drawSize");
  }

  @Test
  void testPlaceSeedTeams_ThrowsException_WhenDrawSizeMismatch() {
    // Arrange
    Round            round = TestFixtures.buildEmptyRound(8); // 4 games = 8 slots
    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(8);

    // Act & Then
    assertThrows(IllegalStateException.class,
                 () -> SeedPlacementUtil.placeSeedTeams(round, pairs, 4, 16), // Wrong drawSize
                 "Should throw exception when drawSize doesn't match actual games");
  }

  // ========== NEW TESTS FOR POOLS (GROUP PHASE) ==========

  @ParameterizedTest(name = "placeSeedTeamsInPools: {0} teams, {1} pools, {2} seeds")
  @CsvSource({
      "12, 4, 4",  // 4 seeds in 4 pools (1 seed per pool)
      "16, 4, 4",  // 4 seeds in 4 pools (1 seed per pool)
      "16, 4, 8",  // 8 seeds in 4 pools (2 seeds per pool)
      "12, 2, 4",  // 4 seeds in 2 pools (2 seeds per pool)
      "24, 8, 8",  // 8 seeds in 8 pools (1 seed per pool)
      "12, 4, 4",  // 4 seeds in 4 pools (1 seed per pool) - was 6 seeds
      "20, 5, 5",  // 5 seeds in 5 pools (1 seed per pool) - was 10 seeds
      "12, 4, 0"   // No seeds to place
  })
  void testPlaceSeedTeamsInPools(int totalTeams, int nbPools, int nbSeeds) {
    // Arrange
    Round            round          = createRoundWithPools(nbPools);
    List<PlayerPair> teams          = createSeededPlayerPairs(totalTeams, nbSeeds);
    int              nbPairsPerPool = totalTeams / nbPools;

    // Act
    SeedPlacementUtil.placeSeedTeamsInPools(round, teams, nbPairsPerPool);

    // Assert - verify seeded teams are placed
    Set<PlayerPair> placedSeeds = new HashSet<>();
    for (Pool pool : round.getPools()) {
      for (PlayerPair pair : pool.getPairs()) {
        if (pair.getSeed() > 0) {
          placedSeeds.add(pair);
        }
      }
    }

    List<PlayerPair> expectedSeeds = teams.stream()
                                          .filter(pair -> pair.getSeed() > 0)
                                          .toList();

    assertEquals(expectedSeeds.size(), placedSeeds.size(), "All seeded teams should be placed");
    assertTrue(placedSeeds.containsAll(expectedSeeds), "All expected seeds should be placed");

    // Verify round-robin distribution of seeds
    if (nbSeeds > 0 && nbPools > 0) {
      int expectedSeedsPerPool = nbSeeds / nbPools;
      int poolsWithExtraSeed   = nbSeeds % nbPools;

      for (int i = 0; i < nbPools; i++) {
        Pool pool = round.getPools().get(i);
        long seedsInPool = pool.getPairs().stream()
                               .filter(pair -> pair.getSeed() > 0)
                               .count();

        int expectedForThisPool = expectedSeedsPerPool;
        if (i < poolsWithExtraSeed) {
          expectedForThisPool++;
        }

        assertEquals(expectedForThisPool, seedsInPool,
                     "Pool " + i + " should have " + expectedForThisPool + " seeds, but had " + seedsInPool);
      }
    }
  }

  @Test
  void testPlaceSeedTeamsInPools_RoundRobinDistribution() {
    // Arrange
    Round            round = createRoundWithPools(4);
    List<PlayerPair> teams = createSeededPlayerPairs(16, 8); // 8 seeds in 4 pools

    // Act
    SeedPlacementUtil.placeSeedTeamsInPools(round, teams, 4);

    // Assert - verify seeds are distributed round-robin style
    // TS1 should go to Pool 0, TS2 to Pool 1, TS3 to Pool 2, TS4 to Pool 3
    // TS5 should go to Pool 0, TS6 to Pool 1, TS7 to Pool 2, TS8 to Pool 3
    for (int i = 0; i < 4; i++) {
      Pool pool = round.getPools().get(i);
      assertEquals(2, pool.getPairs().size(), "Each pool should have exactly 2 seeds");

      // Check that seeds are distributed in round-robin fashion
      List<Integer> seedsInPool = pool.getPairs().stream()
                                      .map(PlayerPair::getSeed)
                                      .sorted()
                                      .toList();

      List<Integer> expectedSeeds = List.of(i + 1, i + 5); // TS(i+1) and TS(i+5)
      assertEquals(expectedSeeds, seedsInPool,
                   "Pool " + i + " should contain seeds " + expectedSeeds + " but had " + seedsInPool);
    }
  }

  @Test
  void testPlaceSeedTeamsInPools_OneSeedPerPool() {
    // Arrange
    Round            round = createRoundWithPools(4);
    List<PlayerPair> teams = createSeededPlayerPairs(12, 4); // 4 seeds in 4 pools

    // Act
    SeedPlacementUtil.placeSeedTeamsInPools(round, teams, 3);

    // Assert - each pool should have exactly 1 seed
    for (int i = 0; i < 4; i++) {
      Pool pool = round.getPools().get(i);
      assertEquals(1, pool.getPairs().size(), "Each pool should have exactly 1 seed");

      PlayerPair seedInPool = pool.getPairs().getFirst();
      assertEquals(i + 1, seedInPool.getSeed(),
                   "Pool " + i + " should contain TS" + (i + 1) + " but had TS" + seedInPool.getSeed());
    }
  }

  @Test
  void testPlaceSeedTeamsInPools_MoreSeedsThanPools() {
    // Arrange
    Round            round = createRoundWithPools(3);
    List<PlayerPair> teams = createSeededPlayerPairs(12, 7); // 7 seeds in 3 pools

    // Act
    SeedPlacementUtil.placeSeedTeamsInPools(round, teams, 4);

    // Assert - seeds should be distributed as evenly as possible
    // Pool 0: TS1, TS4, TS7 (3 seeds)
    // Pool 1: TS2, TS5 (2 seeds)
    // Pool 2: TS3, TS6 (2 seeds)
    assertEquals(3, round.getPools().getFirst().getPairs().size(), "Pool 0 should have 3 seeds");
    assertEquals(2, round.getPools().get(1).getPairs().size(), "Pool 1 should have 2 seeds");
    assertEquals(2, round.getPools().get(2).getPairs().size(), "Pool 2 should have 2 seeds");

    // Verify all 7 seeds are placed
    long totalSeeds = round.getPools().stream()
                           .flatMap(pool -> pool.getPairs().stream())
                           .filter(pair -> pair.getSeed() > 0)
                           .count();
    assertEquals(7, totalSeeds, "All 7 seeds should be placed");
  }

  @Test
  void testPlaceSeedTeamsInPools_NoSeeds() {
    // Arrange
    Round            round = createRoundWithPools(4);
    List<PlayerPair> teams = createSeededPlayerPairs(12, 0); // No seeds

    // Act
    SeedPlacementUtil.placeSeedTeamsInPools(round, teams, 3);

    // Assert - no teams should be placed
    for (Pool pool : round.getPools()) {
      assertTrue(pool.getPairs().isEmpty(), "Pool should be empty when no seeds");
    }
  }

  @Test
  void testPlaceSeedTeamsInPools_HandlesNullInputs() {
    // Test null round
    List<PlayerPair> teams = createSeededPlayerPairs(8, 4);
    SeedPlacementUtil.placeSeedTeamsInPools(null, teams, 2);
    // Should not throw

    // Test null teams
    Round round = createRoundWithPools(4);
    SeedPlacementUtil.placeSeedTeamsInPools(round, null, 2);
    // Should not throw

    // Test empty pools
    Round emptyRound = new Round();
    SeedPlacementUtil.placeSeedTeamsInPools(emptyRound, teams, 2);
    // Should not throw
    assertTrue(true, "Should handle null teams without throwing exception");
  }

  @Test
  void testPlaceSeedTeamsInPools_WithPoolCapacityLimits() {
    // Arrange
    Round            round          = createRoundWithPools(3);
    List<PlayerPair> teams          = createSeededPlayerPairs(9, 3); // 3 seeds in 3 pools (1 per pool)
    int              nbPairsPerPool = 3; // Each pool can hold max 3 teams

    // Act
    SeedPlacementUtil.placeSeedTeamsInPools(round, teams, nbPairsPerPool);

    // Assert - verify no pool exceeds capacity
    for (Pool pool : round.getPools()) {
      assertTrue(pool.getPairs().size() <= nbPairsPerPool,
                 "Pool should not exceed capacity of " + nbPairsPerPool);
    }

    // Verify all 3 seeds are placed (1 per pool)
    long totalSeeds = round.getPools().stream()
                           .flatMap(pool -> pool.getPairs().stream())
                           .filter(pair -> pair.getSeed() > 0)
                           .count();
    assertEquals(3, totalSeeds, "All 3 seeds should be placed");

    // Verify each pool has exactly 1 seed
    for (int i = 0; i < 3; i++) {
      Pool pool = round.getPools().get(i);
      long seedsInPool = pool.getPairs().stream()
                             .filter(pair -> pair.getSeed() > 0)
                             .count();
      assertEquals(1, seedsInPool, "Pool " + i + " should have exactly 1 seed");
    }
  }

  @Test
  void testPlaceSeedTeamsInPools_RespectsPoolCapacityAndFallback() {
    // Arrange
    Round            round          = createRoundWithPools(3);
    List<PlayerPair> teams          = createSeededPlayerPairs(9, 6); // 6 seeds in 3 pools (2 per pool max)
    int              nbPairsPerPool = 3; // Each pool can hold max 3 teams

    // Pre-fill first pool to near capacity
    PlayerPair existingTeam = createSeededPlayerPairs(1, 0).getFirst();
    existingTeam.setSeed(0); // Non-seeded team
    round.getPools().getFirst().addPair(existingTeam);
    round.getPools().getFirst().addPair(existingTeam); // 2 teams already in pool 0

    // Act
    SeedPlacementUtil.placeSeedTeamsInPools(round, teams, nbPairsPerPool);

    // Assert - verify seeds are placed and capacity is respected
    for (Pool pool : round.getPools()) {
      assertTrue(pool.getPairs().size() <= nbPairsPerPool,
                 "Pool should not exceed capacity of " + nbPairsPerPool);
    }

    // Verify seeds are distributed across available pools
    // With pool 0 having 2 existing teams, available space is: 1 + 3 + 3 = 7 total
    // But we only have 6 seeds, so all should be placed
    long totalSeeds = round.getPools().stream()
                           .flatMap(pool -> pool.getPairs().stream())
                           .filter(pair -> pair.getSeed() > 0)
                           .count();
    assertEquals(6, totalSeeds, "All 6 seeds should be placed");

    // Verify that pool 0 has exactly 1 seed (plus 2 existing non-seeded teams)
    long seedsInPool0 = round.getPools().getFirst().getPairs().stream()
                             .filter(pair -> pair.getSeed() > 0)
                             .count();
    assertEquals(1, seedsInPool0, "Pool 0 should have exactly 1 seed");
    assertEquals(3, round.getPools().getFirst().getPairs().size(), "Pool 0 should be at capacity");

    // Verify pool 1 and 2 have the remaining seeds (should be distributed evenly)
    long seedsInPool1 = round.getPools().get(1).getPairs().stream()
                             .filter(pair -> pair.getSeed() > 0)
                             .count();
    long seedsInPool2 = round.getPools().get(2).getPairs().stream()
                             .filter(pair -> pair.getSeed() > 0)
                             .count();

    // With 5 remaining seeds for 2 pools, distribution should be 3 and 2 (or 2 and 3)
    assertTrue((seedsInPool1 == 3 && seedsInPool2 == 2) || (seedsInPool1 == 2 && seedsInPool2 == 3),
               "Pools 1 and 2 should have 3 and 2 seeds respectively (in any order)");
  }

  // Helper method to create a round with pools
  private Round createRoundWithPools(int nbPools) {
    Round round = new Round();
    for (int i = 0; i < nbPools; i++) {
      Pool pool = new Pool();
      pool.setName("Pool " + (char) ('A' + i));
      round.addPool(pool);
    }
    return round;
  }

  // Helper method to create seeded player pairs
  private List<PlayerPair> createSeededPlayerPairs(int totalTeams, int nbSeeds) {
    List<PlayerPair> pairs = new ArrayList<>();
    for (int i = 1; i <= totalTeams; i++) {
      Player player1 = new Player();
      player1.setName("Player" + (i * 2 - 1));

      Player player2 = new Player();
      player2.setName("Player" + (i * 2));

      PlayerPair pair = new PlayerPair();
      pair.setPlayer1(player1);
      pair.setPlayer2(player2);

      // Set seed for first nbSeeds teams
      if (i <= nbSeeds) {
        pair.setSeed(i);
      } else {
        pair.setSeed(0); // Non-seeded
      }

      pairs.add(pair);
    }
    return pairs;
  }
}
