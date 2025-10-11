package io.github.redouanebali.generation.util;

import io.github.redouanebali.generation.util.GameSlotUtil.TeamSlot;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for handling staggered seed placement in tournaments.
 */
public class StaggeredSeedPlacementUtil {

  private StaggeredSeedPlacementUtil() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Place seeds teams for staggered entry tournaments with proper stage management.
   */
  public static void placeSeedTeamsStaggered(Round round,
                                             List<PlayerPair> playerPairs,
                                             Stage currentStage,
                                             int mainDrawSize,
                                             int totalSeeds,
                                             boolean isFirstRound) {
    if (round == null || round.getGames() == null || playerPairs == null) {
      return;
    }

    if (isFirstRound) {
      // First round of main draw: no seeds enter
      // Seed positions are filled with QUALIFIER placeholders
      placeQualifierPlaceholders(round, totalSeeds);
      return;
    }

    // Following rounds: determine which seeds enter
    int seedsEnteringAtThisStage = getSeedsEnteringAtStage(currentStage, mainDrawSize, totalSeeds);
    int seedsAlreadyEntered      = getSeedsEnteredBeforeStage(currentStage, mainDrawSize, totalSeeds);

    if (seedsEnteringAtThisStage == 0) {
      return;
    }

    final List<PlayerPair> sortedBySeed = new ArrayList<>(playerPairs);
    sortedBySeed.sort(SeedPlacementUtil.getSeedComparator());

    // Get positions for seeds entering at this stage
    final List<Integer> seedSlots = SeedPlacementUtil.getSeedsPositions(round.getGames().size() * 2, seedsEnteringAtThisStage);

    // Place the seeds that enter at this stage
    SeedPlacementUtil.placeTeamsAtSlots(round.getGames(),
                                        sortedBySeed.subList(seedsAlreadyEntered,
                                                             Math.min(seedsAlreadyEntered + seedsEnteringAtThisStage, sortedBySeed.size())),
                                        seedSlots,
                                        true);
  }

  /**
   * Place QUALIFIER placeholders for seeds that will enter in later rounds
   */
  private static void placeQualifierPlaceholders(Round round, int totalSeeds) {
    if (totalSeeds == 0) {
      return;
    }

    // All seeds enter later, so we place QUALIFIERs at their positions
    final List<Integer> allSeedSlots = SeedPlacementUtil.getSeedsPositions(round.getGames().size() * 2, totalSeeds);

    for (int slot : allSeedSlots) {
      TeamSlot   teamSlot  = GameSlotUtil.getTeamSlot(slot);
      Game       game      = round.getGames().get(teamSlot.gameIndex());
      PlayerPair qualifier = PlayerPair.qualifier();

      // Only place if slot is empty
      if (GameSlotUtil.isSlotEmpty(game, teamSlot.side())) {
        GameSlotUtil.setTeam(game, teamSlot.side(), qualifier);
      }
    }
  }

  /**
   * Get number of seeds that enter at a specific stage in staggered entry mode
   */
  public static int getSeedsEnteringAtStage(Stage stage, int mainDrawSize, int totalSeeds) {
    if (totalSeeds <= 0) {
      return 0;
    }

    // For 64-draw with 16 seeds: TS1-8 enter at R64, TS9-16 enter at R32
    // For 32-draw with 16 seeds: TS1-8 enter at R32, TS9-16 enter at R16
    Stage firstSeedsEnterAt  = Stage.fromNbTeams(mainDrawSize);     // R64 for 64-draw, R32 for 32-draw
    Stage secondSeedsEnterAt = Stage.fromNbTeams(mainDrawSize / 2); // R32 for 64-draw, R16 for 32-draw

    if (stage == firstSeedsEnterAt) {
      return Math.min(totalSeeds, totalSeeds / 2); // Top half of seeds
    } else if (stage == secondSeedsEnterAt) {
      return Math.max(0, totalSeeds - totalSeeds / 2); // Bottom half of seeds
    }

    return 0; // No seeds enter at this stage
  }

  /**
   * Get number of seeds that already entered before this stage
   */
  public static int getSeedsEnteredBeforeStage(Stage stage, int mainDrawSize, int totalSeeds) {
    if (totalSeeds <= 0) {
      return 0;
    }

    Stage firstSeedsEnterAt  = Stage.fromNbTeams(mainDrawSize);
    Stage secondSeedsEnterAt = Stage.fromNbTeams(mainDrawSize / 2);

    if (stage == secondSeedsEnterAt) {
      return Math.min(totalSeeds, totalSeeds / 2); // Top seeds already entered
    }

    return 0; // No seeds entered yet
  }
}
