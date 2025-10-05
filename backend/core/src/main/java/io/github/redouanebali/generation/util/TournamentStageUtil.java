package io.github.redouanebali.generation.util;

import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import java.util.List;

/**
 * Utility class for tournament stage operations. Centralizes logic for determining initial rounds, first main draw stages, etc.
 */
public class TournamentStageUtil {

  /**
   * Determines if a round is an initial round where players first enter the tournament. Initial rounds are: - Q1 (first qualification round) - GROUPS
   * (group phase) - First main draw round (R64, R32, R16, etc. depending on tournament structure)
   */
  public static boolean isInitialRound(Stage stage) {
    return stage == Stage.Q1 ||
           stage == Stage.GROUPS ||
           isFirstMainDrawStage(stage);
  }

  /**
   * Determines if a stage is a potential first main draw stage. This checks if the stage is any main draw stage, but doesn't verify if it's actually
   * the first one in a specific tournament.
   */
  public static boolean isFirstMainDrawStage(Stage stage) {
    return stage == Stage.R64 ||
           stage == Stage.R32 ||
           stage == Stage.R16 ||
           stage == Stage.QUARTERS ||
           stage == Stage.SEMIS ||
           stage == Stage.FINAL;
  }

  /**
   * Determines the first main draw stage that actually exists in the tournament. This looks at the tournament rounds to find the largest main draw
   * stage present.
   */
  public static Stage findFirstMainDrawStage(List<Round> rounds) {
    if (rounds == null || rounds.isEmpty()) {
      return null;
    }

    // Check stages in order from largest to smallest
    Stage[] mainDrawStages = {Stage.R64, Stage.R32, Stage.R16, Stage.QUARTERS, Stage.SEMIS, Stage.FINAL};

    for (Stage stage : mainDrawStages) {
      boolean stageExists = rounds.stream()
                                  .anyMatch(round -> round.getStage() == stage);
      if (stageExists) {
        return stage;
      }
    }

    return null;
  }

  /**
   * Determines if a specific round is an initial round in the context of a tournament. This is more precise than isInitialRound(Stage) as it
   * considers the actual tournament structure.
   */
  public static boolean isInitialRoundInTournament(Round round, List<Round> allRounds) {
    Stage stage = round.getStage();

    // Q1 and GROUPS are always initial rounds
    if (stage == Stage.Q1 || stage == Stage.GROUPS) {
      return true;
    }

    // For main draw, check if this is the first main draw stage in this tournament
    Stage firstMainDrawStage = findFirstMainDrawStage(allRounds);
    return stage == firstMainDrawStage;
  }
}
