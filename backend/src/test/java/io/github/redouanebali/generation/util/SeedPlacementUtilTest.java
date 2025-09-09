package io.github.redouanebali.generation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.util.TestFixtures;
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
    assertEquals(expectedTS1, seedPositions.get(0), "TS1 must always be at position " + expectedTS1);
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
    List<PlayerPair> pairs = TestFixtures.createPairs(drawSize);
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
    List<PlayerPair> pairs = TestFixtures.createPairs(drawSize);

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
    List<PlayerPair> pairs = TestFixtures.createPairs(8);

    // Manually place a team at seed position 0 (game 0, team A)
    round.getGames().get(0).setTeamA(pairs.get(7)); // Use a different pair

    // Act & Then
    assertThrows(IllegalStateException.class,
                 () -> SeedPlacementUtil.placeSeedTeams(round, pairs, 4, 8),
                 "Should throw exception when seed slot is already occupied");
  }

  @Test
  void testPlaceSeedTeams_HandlesNullInputs() {
    // Test null round
    SeedPlacementUtil.placeSeedTeams(null, TestFixtures.createPairs(8), 4, 8);
    // Should not throw

    // Test null games - should use nbSeeds=0 since no games can be placed
    Round round = new Round();
    round.replaceGames(null);
    SeedPlacementUtil.placeSeedTeams(round, TestFixtures.createPairs(8), 0, 0);
    // Should not throw

    // Test null players
    Round validRound = TestFixtures.buildEmptyRound(8);
    SeedPlacementUtil.placeSeedTeams(validRound, null, 4, 8);
    // Should not throw

    // Test nbSeeds = 0
    Round anotherValidRound = TestFixtures.buildEmptyRound(8);
    SeedPlacementUtil.placeSeedTeams(anotherValidRound, TestFixtures.createPairs(8), 0, 8);
    // Should not throw and should not place anything
    for (Game g : anotherValidRound.getGames()) {
      assertNull(g.getTeamA());
      assertNull(g.getTeamB());
    }
  }

  @Test
  void testPlaceSeedTeams_ThrowsException_ForNegativeParameters() {
    // Arrange
    Round            round = TestFixtures.buildEmptyRound(8);
    List<PlayerPair> pairs = TestFixtures.createPairs(8);

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
    List<PlayerPair> pairs = TestFixtures.createPairs(8);

    // Act & Then
    assertThrows(IllegalStateException.class,
                 () -> SeedPlacementUtil.placeSeedTeams(round, pairs, 4, 16), // Wrong drawSize
                 "Should throw exception when drawSize doesn't match actual games");
  }
}
