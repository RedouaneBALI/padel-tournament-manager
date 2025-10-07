package io.github.redouanebali.generation.util;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for randomly placing remaining teams in tournaments. Extracted logic from KnockoutPhase to make it reusable.
 */
public class RandomPlacementUtil {

  /**
   * Randomly places remaining teams in the empty slots of a round. Supports both knockout rounds (with games) and group rounds (with pools).
   *
   * @param round the round to place teams in
   * @param remainingTeams the teams to place randomly
   * @throws IllegalArgumentException if totalPairs exceeds available slots
   */
  public static void placeRemainingTeamsRandomly(Round round, List<PlayerPair> remainingTeams) {
    if (round == null || remainingTeams == null || remainingTeams.isEmpty()) {
      return;
    }

    // Check if this is a group round (has pools) or knockout round (has games)
    if (!round.getPools().isEmpty()) {
      placeRemainingTeamsInPools(round, remainingTeams);
    } else if (round.getGames() != null && !round.getGames().isEmpty()) {
      placeRemainingTeamsInGames(round, remainingTeams);
    }
  }

  /**
   * Places remaining teams randomly in pools for group phase.
   */
  private static void placeRemainingTeamsInPools(Round round, List<PlayerPair> remainingTeams) {
    List<Pool> pools = round.getPools();
    if (pools.isEmpty()) {
      return;
    }

    List<PlayerPair> teamsToPlace = filterUnplacedTeams(pools, remainingTeams);
    if (teamsToPlace.isEmpty()) {
      return;
    }

    distributeTeamsAcrossPools(pools, teamsToPlace);
  }

  /**
   * Filters out teams that are already placed in pools.
   */
  private static List<PlayerPair> filterUnplacedTeams(List<Pool> pools, List<PlayerPair> remainingTeams) {
    List<PlayerPair> alreadyPlaced = pools.stream()
                                          .flatMap(pool -> pool.getPairs().stream())
                                          .toList();

    return remainingTeams.stream()
                         .filter(team -> !alreadyPlaced.contains(team))
                         .toList();
  }

  /**
   * Distributes teams evenly across pools using round-robin distribution.
   */
  private static void distributeTeamsAcrossPools(List<Pool> pools, List<PlayerPair> teamsToPlace) {
    List<PlayerPair> shuffled = new ArrayList<>(teamsToPlace);
    Collections.shuffle(shuffled);

    int teamIndex = 0;
    int poolIndex = 0;

    while (teamIndex < shuffled.size()) {
      Pool currentPool = pools.get(poolIndex);
      currentPool.addPair(shuffled.get(teamIndex));

      teamIndex++;
      poolIndex = (poolIndex + 1) % pools.size(); // Move to next pool, wrap around
    }
  }

  /**
   * Places remaining teams randomly in games for knockout phase.
   */
  private static void placeRemainingTeamsInGames(Round round, List<PlayerPair> remainingTeams) {
    if (remainingTeams.isEmpty()) {
      return;
    }

    List<PlayerPair> shuffled      = shuffleTeams(remainingTeams);
    int              unplacedCount = placeTeamsInGames(round.getGames(), shuffled);

    if (unplacedCount > 0) {
      logUnplacedTeamsWarning(unplacedCount);
    }
  }

  /**
   * Shuffles teams for random placement.
   */
  private static List<PlayerPair> shuffleTeams(List<PlayerPair> teams) {
    List<PlayerPair> shuffled = new ArrayList<>(teams);
    Collections.shuffle(shuffled);
    return shuffled;
  }

  /**
   * Places teams sequentially in available game slots.
   *
   * @return number of teams that couldn't be placed
   */
  private static int placeTeamsInGames(List<Game> games, List<PlayerPair> shuffledTeams) {
    int index = 0;

    for (Game game : games) {
      if (index >= shuffledTeams.size()) {
        break;
      }
      if (game.getTeamA() == null) {
        game.setTeamA(shuffledTeams.get(index++));
      }
      if (index >= shuffledTeams.size()) {
        break;
      }
      if (game.getTeamB() == null) {
        game.setTeamB(shuffledTeams.get(index++));
      }
    }

    return shuffledTeams.size() - index;
  }

  /**
   * Logs a warning when not all teams could be placed.
   */
  private static void logUnplacedTeamsWarning(int unplacedCount) {
    System.err.println("WARNING: Could not place all teams. " + unplacedCount + " teams remaining.");
  }

  /**
   * Randomly places the required number of QUALIFIER placeholders in available slots of a round.
   *
   * @param round the round to place qualifiers in
   * @param nbQualifiers number of qualifiers to place
   */
  public static void placeQualifiers(Round round, int nbQualifiers) {
    if (round == null || nbQualifiers <= 0) {
      return;
    }

    List<Slot> availableSlots = collectAvailableSlots(round.getGames());
    Collections.shuffle(availableSlots);
    placeQualifiersInSlots(availableSlots, nbQualifiers);
  }

  /**
   * Collects all available slots from games.
   */
  private static List<Slot> collectAvailableSlots(List<Game> games) {
    List<Slot> availableSlots = new ArrayList<>();
    for (Game game : games) {
      if (game.getTeamA() == null) {
        availableSlots.add(new Slot(game, true));
      }
      if (game.getTeamB() == null) {
        availableSlots.add(new Slot(game, false));
      }
    }
    return availableSlots;
  }

  /**
   * Places qualifiers in the specified number of slots.
   */
  private static void placeQualifiersInSlots(List<Slot> availableSlots, int nbQualifiers) {
    int placed = 0;
    for (Slot slot : availableSlots) {
      if (placed >= nbQualifiers) {
        break;
      }
      if (slot.isTeamA) {
        slot.game.setTeamA(PlayerPair.qualifier());
      } else {
        slot.game.setTeamB(PlayerPair.qualifier());
      }
      placed++;
    }
  }

  /**
   * Places teams in order (without shuffling) - useful for manual mode.
   */
  public static void placeTeamsInOrder(Round round, List<PlayerPair> teams) {
    if (round == null || teams == null || teams.isEmpty()) {
      return;
    }

    if (round.getGames() != null && !round.getGames().isEmpty()) {
      placeTeamsInOrderInGames(round.getGames(), teams);
    }

    if (!round.getPools().isEmpty()) {
      placeTeamsInOrderInPools(round.getPools(), teams);
    }
  }

  /**
   * Places teams in order in games without shuffling.
   */
  private static void placeTeamsInOrderInGames(List<Game> games, List<PlayerPair> teams) {
    int index = 0;
    for (Game game : games) {
      if (game.getTeamA() == null && index < teams.size()) {
        game.setTeamA(teams.get(index++));
      }
      if (game.getTeamB() == null && index < teams.size()) {
        game.setTeamB(teams.get(index++));
      }
      if (index >= teams.size()) {
        break;
      }
    }
  }

  /**
   * Places teams in order in pools without shuffling.
   */
  private static void placeTeamsInOrderInPools(List<Pool> pools, List<PlayerPair> teams) {
    int nbPools        = pools.size();
    int nbPairsPerPool = teams.size() / nbPools;

    for (int poolIndex = 0; poolIndex < pools.size() && poolIndex < nbPairsPerPool; poolIndex++) {
      Pool currentPool = pools.get(poolIndex);
      int  startIndex  = poolIndex * nbPairsPerPool;
      int  endIndex    = Math.min(startIndex + nbPairsPerPool, teams.size());

      for (int i = startIndex; i < endIndex; i++) {
        System.out.println("adding player " + i + " to pool " + poolIndex);
        currentPool.addPair(teams.get(i));
      }
    }
  }

  // Helper class to represent a slot in a game (teamA or teamB)
  private static class Slot {

    Game    game;
    boolean isTeamA;

    Slot(Game game, boolean isTeamA) {
      this.game    = game;
      this.isTeamA = isTeamA;
    }
  }
}
