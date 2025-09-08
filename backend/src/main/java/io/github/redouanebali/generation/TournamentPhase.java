package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.List;

/**
 * Defines the contract for a tournament phase (qualification or main draw). Provides operations for initializing the structure, placing seeded and
 * non-seeded teams, inserting BYEs, propagating winners, and overriding matches manually when needed.
 */
public interface TournamentPhase {

  /**
   * Validate the tournament configuration for this phase. Checks draw sizes (powers of two), number of seeds, number of qualifiers, compatibility
   * between pre-qualification and main draw, and any phase-specific constraints.
   *
   * @param tournament the tournament whose configuration will be validated
   * @return a list of human-readable validation errors; empty list means the configuration is valid
   */
  List<String> validate(Tournament tournament);

  /**
   * Initialize the tournament phase by creating rounds and games based on the configuration.
   *
   * @param tournament the tournament to initialize
   * @return list of initialized rounds
   */
  List<Round> initialize(Tournament tournament);

  /**
   * Place seeded teams at their theoretical positions in the given round. This method mutates the round's games in place.
   *
   * @param round the round whose games will be updated
   * @param playerPairs the list of all player pairs with seed values (the first nbSeeds by seed rank are considered)
   */
  void placeSeedTeams(Round round, List<PlayerPair> playerPairs);

  /**
   * Renvoie la liste des positions des seeds (utilise les propriétés internes de la classe).
   *
   * @return liste des positions des seeds
   */
  List<Integer> getSeedsPositions();

  /**
   * Insert BYE teams into the bracket if the draw is not full. Ensures seeded players can advance automatically when required. This method mutates
   * the round's games in place.
   *
   * @param round the round whose games will be updated
   * @param totalPairs total number of pairs registered
   */
  void placeByeTeams(Round round, int totalPairs);

  /**
   * Randomly assign remaining non-seeded teams into the empty slots of the draw. This method mutates the round's games in place.
   *
   * @param round the round whose games will be updated
   * @param remainingTeams teams that are not seeded
   */
  void placeRemainingTeamsRandomly(Round round, List<PlayerPair> remainingTeams);

  /**
   * Propagate winners of completed games into the next round of the tournament. This method mutates the provided tournament in place (rounds/games
   * are updated) and does not return a value.
   *
   * @param tournament the tournament whose winners should be advanced to subsequent rounds
   */
  void propagateWinners(Tournament tournament);

  /**
   * Get the initial stage of this phase where players enter the tournament. For example: Q1 for qualifications, R64/R32/R16 for main draw, GROUPS for
   * group phase.
   *
   * @return the stage representing the initial round of this phase
   */
  Stage getInitialStage();
}
