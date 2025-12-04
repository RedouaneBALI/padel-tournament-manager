package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import java.util.List;

/**
 * Defines the contract for a tournament phase (qualification or main draw). Provides operations for initializing the structure, placing seeded and
 * non-seeded teams, inserting BYEs, propagating winners, and overriding matches manually when needed.
 */
public interface TournamentPhase {

  /**
   * Validates the tournament configuration for this phase. Checks draw sizes (powers of two), number of seeds, number of qualifiers, compatibility
   * between pre-qualification and main draw, and any phase-specific constraints.
   *
   * @param tournament the tournament whose configuration will be validated
   * @return a list of human-readable validation errors; an empty list means the configuration is valid
   */
  List<String> validate(Tournament tournament);

  /**
   * Initializes the tournament phase by creating rounds and games based on the configuration.
   *
   * @param config the configuration for the tournament phase
   * @return list of initialized rounds
   */
  List<Round> initialize(TournamentConfig config);

  /**
   * Places seeded teams at their theoretical positions in the given round. This method mutates the round's games in place.
   *
   * @param round the round whose games will be updated
   * @param playerPairs the list of all player pairs with seed values (the first nbSeeds by seed rank are considered)
   */
  void placeSeedTeams(Round round, List<PlayerPair> playerPairs);

  /**
   * Inserts BYE teams into the bracket if the draw is not full. Ensures seeded players can advance automatically when required. This method mutates
   * the round's games in place.
   *
   * @param round the round whose games will be updated
   * @param totalPairs total number of pairs registered
   */
  void placeByeTeams(Round round, int totalPairs);

  /**
   * Randomly assigns remaining non-seeded teams into the empty slots of the draw. This method mutates the round's games in place.
   *
   * @param round the round whose games will be updated
   * @param remainingTeams teams that are not seeded
   */
  void placeRemainingTeamsRandomly(Round round, List<PlayerPair> remainingTeams);

  /**
   * Propagates winners of completed games into the next round of the tournament. This method mutates the provided tournament in place (rounds/games
   * are updated) and does not return a value.
   *
   * @param tournament the tournament whose winners should be advanced to subsequent rounds
   */
  void propagateWinners(Tournament tournament);

  /**
   * Optimized propagation: only propagate from the round containing the given game onwards. This avoids reprocessing all rounds when only one game
   * changes.
   *
   * @param tournament the tournament to update
   * @param game the game that was modified
   */
  default void propagateWinnersFromGame(Tournament tournament, Game game) {
    // Default implementation: fallback to full propagation
    propagateWinners(tournament);
  }

  /**
   * Gets the initial stage of this phase where players enter the tournament. For example: Q1 for qualifications, R64/R32/R16 for main draw, GROUPS
   * for group phase.
   *
   * @return the stage representing the initial round of this phase
   */
  Stage getInitialStage();
}
