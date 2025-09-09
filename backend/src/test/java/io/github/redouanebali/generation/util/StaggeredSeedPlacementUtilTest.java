package io.github.redouanebali.generation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.util.TestFixtures;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class StaggeredSeedPlacementUtilTest {

  @ParameterizedTest(name = "First round: all seed slots are QUALIFIER for drawSize={0}, totalSeeds={1}")
  @CsvSource({
      "64, 16",
      "32, 16",
      "32, 8",
      "16, 8",
      "16, 4"
  })
  void testFirstRound_AllSeedSlotsAreQualifier(int drawSize, int totalSeeds) {
    Round            round = TestFixtures.buildEmptyRound(drawSize);
    List<PlayerPair> pairs = TestFixtures.createPairs(drawSize);
    Stage            stage = Stage.fromNbTeams(drawSize);

    StaggeredSeedPlacementUtil.placeSeedTeamsStaggered(round, pairs, stage, drawSize, totalSeeds, true);

    List<Integer> seedSlots = SeedPlacementUtil.getSeedsPositions(drawSize, totalSeeds);
    for (int slot : seedSlots) {
      int        gameIndex = slot / 2;
      boolean    left      = (slot % 2 == 0);
      Game       g         = round.getGames().get(gameIndex);
      PlayerPair placed    = left ? g.getTeamA() : g.getTeamB();
      assertNotNull(placed, "Slot should not be null");
      assertEquals(PairType.QUALIFIER, placed.getType(), "Slot should be QUALIFIER");
    }
  }

  @ParameterizedTest(name = "Second round: correct seeds are placed for drawSize={0}, totalSeeds={1}")
  @CsvSource({
      "64, 16",
      "32, 16",
      "32, 8",
      "16, 8",
      "16, 4"
  })
  void testSecondRound_SeedsArePlaced(int drawSize, int totalSeeds) {
    Round            round = TestFixtures.buildEmptyRound(drawSize / 2);
    List<PlayerPair> pairs = TestFixtures.createPairs(drawSize);
    Stage            stage = Stage.fromNbTeams(drawSize / 2);

    // Simule le fait que la moitié des seeds sont déjà entrés
    StaggeredSeedPlacementUtil.placeSeedTeamsStaggered(round, pairs, stage, drawSize, totalSeeds, false);

    int             seedsEntering = StaggeredSeedPlacementUtil.getSeedsEnteringAtStage(stage, drawSize, totalSeeds);
    List<Integer>   seedSlots     = SeedPlacementUtil.getSeedsPositions(drawSize / 2, seedsEntering);
    Set<PlayerPair> placedSeeds   = new HashSet<>();
    for (int i = 0; i < seedSlots.size(); i++) {
      int        slot      = seedSlots.get(i);
      int        gameIndex = slot / 2;
      boolean    left      = (slot % 2 == 0);
      Game       g         = round.getGames().get(gameIndex);
      PlayerPair placed    = left ? g.getTeamA() : g.getTeamB();
      assertNotNull(placed, "Seed slot should not be null");
      assertTrue(placed.getSeed() > 0, "Placed pair should be a seed");
      placedSeeds.add(placed);
    }
    assertEquals(seedsEntering, placedSeeds.size(), "Should have placed exactly the entering seeds");
  }

  @Test
  void testNoSeeds_NoPlacementOrQualifier() {
    int              drawSize = 16;
    Round            round    = TestFixtures.buildEmptyRound(drawSize);
    List<PlayerPair> pairs    = TestFixtures.createPairs(drawSize);
    Stage            stage    = Stage.fromNbTeams(drawSize);

    StaggeredSeedPlacementUtil.placeSeedTeamsStaggered(round, pairs, stage, drawSize, 0, true);
    for (Game g : round.getGames()) {
      assertNull(g.getTeamA());
      assertNull(g.getTeamB());
    }
  }

  @Test
  void testSeedsAlreadyEntered_NotPlacedAgain() {
    int              drawSize   = 32;
    int              totalSeeds = 8;
    Round            round      = TestFixtures.buildEmptyRound(drawSize / 2);
    List<PlayerPair> pairs      = TestFixtures.createPairs(drawSize);
    Stage            stage      = Stage.fromNbTeams(drawSize / 2);

    // Place seeds as if they already entered in previous round
    int seedsAlreadyEntered = StaggeredSeedPlacementUtil.getSeedsEnteredBeforeStage(stage, drawSize, totalSeeds);
    // Remove already entered seeds from the list to simulate their entry
    List<PlayerPair> enteringSeeds = pairs.subList(seedsAlreadyEntered, Math.min(seedsAlreadyEntered + totalSeeds, pairs.size()));

    StaggeredSeedPlacementUtil.placeSeedTeamsStaggered(round, enteringSeeds, stage, drawSize, totalSeeds, false);

    // Only the new seeds should be placed
    int           seedsEntering = StaggeredSeedPlacementUtil.getSeedsEnteringAtStage(stage, drawSize, totalSeeds);
    List<Integer> seedSlots     = SeedPlacementUtil.getSeedsPositions(drawSize / 2, seedsEntering);
    int           count         = 0;
    for (int slot : seedSlots) {
      int        gameIndex = slot / 2;
      boolean    left      = (slot % 2 == 0);
      Game       g         = round.getGames().get(gameIndex);
      PlayerPair placed    = left ? g.getTeamA() : g.getTeamB();
      if (placed != null && placed.getSeed() > 0) {
        count++;
      }
    }
    assertEquals(seedsEntering, count, "Should only place the new seeds");
  }

  @Test
  void testHandlesNullInputs() {
    // Null round
    StaggeredSeedPlacementUtil.placeSeedTeamsStaggered(null, null, null, 0, 0, true);
    // Null games
    Round round = new Round();
    round.replaceGames(null);
    StaggeredSeedPlacementUtil.placeSeedTeamsStaggered(round, null, null, 0, 0, true);
    // Null playerPairs
    Round validRound = TestFixtures.buildEmptyRound(8);
    StaggeredSeedPlacementUtil.placeSeedTeamsStaggered(validRound, null, Stage.fromNbTeams(8), 8, 4, true);
    // Should not throw
  }
}

