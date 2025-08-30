package io.github.redouanebali.generationV2;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
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
   * @param nbSeeds number of seeds to place
   */
  void placeSeedTeams(Round round, List<PlayerPair> playerPairs, int nbSeeds);

  /**
   * Compute recursively the positions of the seeds in a tournament
   *
   * @param drawSize size of the bracket
   * @param nbSeeds number of seeds teams
   * @return list of the positions of the seeds
   */
  List<Integer> getSeedsPositions(int drawSize, int nbSeeds);

  /**
   * Insert BYE teams into the bracket if the draw is not full. Ensures seeded players can advance automatically when required. This method mutates
   * the round's games in place.
   *
   * @param round the round whose games will be updated
   * @param totalPairs total number of pairs registered
   * @param drawSize the size of the draw (power of 2)
   * @param nbSeeds number of seeds
   */
  void placeByeTeams(Round round, int totalPairs, int drawSize, int nbSeeds);

  /**
   * Randomly assign remaining non-seeded teams into the empty slots of the draw. This method mutates the round's games in place.
   *
   * @param round the round whose games will be updated
   * @param remainingTeams teams that are not seeded
   */
  void placeRemainingTeamsRandomly(Round round, List<PlayerPair> remainingTeams);

  /**
   * Propagate winners of completed games into the next round of the tournament.
   *
   * @param tournament the tournament with results to propagate
   * @return the updated tournament with winners advanced
   */
  Tournament propagateWinners(Tournament tournament);

  /**
   * Replace or initialize games for a given round, used in manual mode.
   *
   * @param round the round to update
   * @param games list of games to set
   * @return the updated round with provided games
   */
  Round setRoundGames(Round round, List<Game> games);

}
