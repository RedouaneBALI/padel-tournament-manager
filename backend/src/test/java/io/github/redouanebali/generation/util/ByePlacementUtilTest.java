package io.github.redouanebali.generation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.util.TestFixtures;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ByePlacementUtilTest {

  @ParameterizedTest(name = "placeByeTeams AUTO: drawSize={0}, nbSeeds={1}, totalPairs={2}")
  @CsvSource({
      // drawSize, nbSeeds, totalPairs
      "32, 16, 24",
      "32, 8, 20",
      "16, 8, 12",
      "64, 16, 48"
  })
  void testPlaceByeTeams_AutoSeeds(int drawSize, int nbSeeds, int totalPairs) {
    // Arrange: build empty round and create seeded pairs
    Round            round = TestFixtures.buildEmptyRound(drawSize);
    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(drawSize);
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    // Place seeds first (to simulate automatic mode)
    SeedPlacementUtil.placeSeedTeams(round, pairs, nbSeeds, drawSize);

    // Act: place BYEs based on the number of registered pairs
    ByePlacementUtil.placeByeTeams(round, totalPairs, nbSeeds, drawSize);

    // Assert: BYE count
    int expectedByes = drawSize - totalPairs;
    long actualByes = round.getGames().stream()
                           .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                           .filter(p -> p != null && p.isBye())
                           .count();
    assertEquals(expectedByes, actualByes, "Unexpected BYE count in AUTO mode");

    // BYEs should face the top seeds, in order, until BYEs are exhausted
    List<Integer> seedSlots = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);
    for (int i = 0; i < Math.min(expectedByes, seedSlots.size()); i++) {
      int     slot      = seedSlots.get(i);
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);
      Game    g         = round.getGames().get(gameIndex);

      if (left) {
        assertTrue(g.getTeamB() != null && g.getTeamB().isBye(),
                   "BYE must face seed#" + (i + 1));
      } else {
        assertTrue(g.getTeamA() != null && g.getTeamA().isBye(),
                   "BYE must face seed#" + (i + 1));
      }
    }
  }

  @ParameterizedTest(name = "placeByeTeams MANUAL (declared seeds, none placed): drawSize={0}, nbSeeds={1}, totalPairs={2}")
  @CsvSource({
      // drawSize, nbSeeds, totalPairs
      "32, 16, 24",  // 8 BYEs opposite top 8 seed slots
      "64, 16, 40",  // 24 BYEs; start opposite seeds then remaining distribution
      "64, 32, 48"   // 16 BYEs opposite 32 seed slots (first 16 covered)
  })
  void testPlaceByeTeams_ManualDeclaredSeedsButNotPlaced(int drawSize, int nbSeeds, int totalPairs) {
    // Arrange: round is empty; admin declared seeds count, but hasn't placed teams yet
    Round round = TestFixtures.buildEmptyRound(drawSize);

    // Act
    ByePlacementUtil.placeByeTeams(round, totalPairs, nbSeeds, drawSize);

    // Assert: BYE count
    int expectedByes = drawSize - totalPairs;
    long actualByes = round.getGames().stream()
                           .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                           .filter(p -> p != null && p.isBye())
                           .count();
    assertEquals(expectedByes, actualByes, "Unexpected BYE count in MANUAL declared-seeds mode");

    // Verify that BYEs are placed opposite as many seeds as possible
    List<Integer> seedSlots              = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);
    int           byesPlacedOppositeSeed = 0;

    for (int i = 0; i < seedSlots.size(); i++) {
      int     slot      = seedSlots.get(i);
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);
      Game    g         = round.getGames().get(gameIndex);

      if (left) {
        if (g.getTeamB() != null && g.getTeamB().isBye()) {
          byesPlacedOppositeSeed++;
        }
      } else {
        if (g.getTeamA() != null && g.getTeamA().isBye()) {
          byesPlacedOppositeSeed++;
        }
      }
    }

    // Should have placed at least min(expectedByes, seedSlots.size()) BYEs opposite seeds
    assertTrue(byesPlacedOppositeSeed >= Math.min(expectedByes, seedSlots.size()),
               "Should have at least " + Math.min(expectedByes, seedSlots.size()) +
               " BYEs opposite seeds, but had " + byesPlacedOppositeSeed);
  }

  @Test
  void testPlaceByeTeams_NoByesNeeded_WhenDrawIsFull() {
    // Arrange
    Round round = TestFixtures.buildEmptyRound(8);

    // Act - totalPairs equals drawSize, so no BYEs needed
    ByePlacementUtil.placeByeTeams(round, 8, 4, 8);

    // Assert - no BYEs should be placed
    long actualByes = round.getGames().stream()
                           .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                           .filter(p -> p != null && p.isBye())
                           .count();
    assertEquals(0, actualByes, "No BYEs should be placed when draw is full");
  }

  @Test
  void testPlaceByeTeams_HandlesExistingByes() {
    // Arrange
    Round round = TestFixtures.buildEmptyRound(8);

    // Manually place some BYEs first
    round.getGames().get(0).setTeamA(PlayerPair.bye());
    round.getGames().get(1).setTeamB(PlayerPair.bye());

    // Act - should account for existing BYEs
    ByePlacementUtil.placeByeTeams(round, 4, 2, 8); // Need 4 more BYEs total

    // Assert - should have exactly 4 BYEs total (2 existing + 2 new)
    long actualByes = round.getGames().stream()
                           .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                           .filter(p -> p != null && p.isBye())
                           .count();
    assertEquals(4, actualByes, "Should account for existing BYEs");
  }

  @Test
  void testPlaceByeTeams_ThrowsException_ForInvalidInputs() {
    // Test null round
    assertThrows(IllegalArgumentException.class,
                 () -> ByePlacementUtil.placeByeTeams(null, 4, 2, 8),
                 "Should throw for null round");

    // Test null games
    Round round = new Round();
    round.replaceGames(null);
    assertThrows(IllegalArgumentException.class,
                 () -> ByePlacementUtil.placeByeTeams(round, 4, 2, 8),
                 "Should throw for null games");

    // Test drawSize mismatch
    Round validRound = TestFixtures.buildEmptyRound(8); // 4 games = 8 slots
    assertThrows(IllegalStateException.class,
                 () -> ByePlacementUtil.placeByeTeams(validRound, 4, 2, 16), // Wrong drawSize
                 "Should throw for drawSize mismatch");

    // Test non-power-of-two drawSize
    assertThrows(IllegalArgumentException.class,
                 () -> ByePlacementUtil.placeByeTeams(validRound, 4, 2, 7), // Not power of 2
                 "Should throw for non-power-of-two drawSize");

    // Test totalPairs exceeding drawSize
    assertThrows(IllegalArgumentException.class,
                 () -> ByePlacementUtil.placeByeTeams(validRound, 10, 2, 8), // totalPairs > drawSize
                 "Should throw when totalPairs exceeds drawSize");
  }

  @Test
  void testPlaceByeTeams_FallbackPlacement() {
    // Arrange - create scenario where not all BYEs can be placed opposite seeds
    Round round = TestFixtures.buildEmptyRound(8);

    // Place some teams at seed positions to block BYE placement
    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(8);
    round.getGames().get(0).setTeamA(pairs.get(0)); // Blocks position 0
    round.getGames().get(3).setTeamB(pairs.get(1)); // Blocks position 7

    // Act - need 4 BYEs but some seed positions are blocked
    ByePlacementUtil.placeByeTeams(round, 4, 4, 8);

    // Assert - should still place correct number of BYEs using fallback logic
    long actualByes = round.getGames().stream()
                           .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                           .filter(p -> p != null && p.isBye())
                           .count();
    assertEquals(4, actualByes, "Should place correct number of BYEs using fallback");
  }

  @Test
  void testPlaceByeTeams_LastResortPlacement() {
    // Arrange - create scenario that forces last resort BYE vs BYE placement
    Round round = TestFixtures.buildEmptyRound(4); // 2 games = 4 slots

    // Block most positions to force BYE vs BYE
    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(4);
    round.getGames().get(0).setTeamA(pairs.get(0));

    // Act - need 3 BYEs but only 3 slots available
    ByePlacementUtil.placeByeTeams(round, 1, 2, 4);

    // Assert - should place all BYEs even if some are BYE vs BYE
    long actualByes = round.getGames().stream()
                           .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                           .filter(p -> p != null && p.isBye())
                           .count();
    assertEquals(3, actualByes, "Should place all BYEs using last resort if needed");
  }

  @Test
  void testPlaceByeTeams_ThrowsException_WhenNotEnoughSlots() {
    // Arrange - fill all but one slot
    Round            round = TestFixtures.buildEmptyRound(4); // 2 games = 4 slots
    List<PlayerPair> pairs = TestFixtures.createPlayerPairs(4);

    round.getGames().get(0).setTeamA(pairs.get(0));
    round.getGames().get(0).setTeamB(pairs.get(1));
    round.getGames().get(1).setTeamA(pairs.get(2));
    // Only 1 slot left

    // Act & Assert - should throw when trying to place 2 BYEs in 1 slot
    assertThrows(IllegalStateException.class,
                 () -> ByePlacementUtil.placeByeTeams(round, 2, 2, 4),
                 "Should throw when not enough empty slots for all BYEs");
  }
}
