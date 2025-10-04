package io.github.redouanebali.generation.util;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    // Filter out teams that are already placed in pools
    List<PlayerPair> alreadyPlaced = pools.stream()
                                          .flatMap(pool -> pool.getPairs().stream())
                                          .toList();

    List<PlayerPair> teamsToPlace = remainingTeams.stream()
                                                  .filter(team -> !alreadyPlaced.contains(team))
                                                  .toList();

    if (teamsToPlace.isEmpty()) {
      return;
    }

    // Shuffle teams for random placement
    List<PlayerPair> shuffled = new ArrayList<>(teamsToPlace);
    Collections.shuffle(shuffled);

    // Distribute teams evenly across pools using round-robin distribution
    // This ensures teams are spread as evenly as possible across all pools
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
    // Calculate available slots
    int availableSlots = (int) round.getGames().stream()
                                    .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                    .filter(Objects::isNull)
                                    .count();

    // Log warning if more teams than slots (but don't throw - just place what we can)
    if (remainingTeams.size() > availableSlots) {
      // Could add logging here in the future
    }

    // Shuffle teams for random placement
    List<PlayerPair> shuffled = new ArrayList<>(remainingTeams);
    Collections.shuffle(shuffled);

    int index = 0;
    for (Game game : round.getGames()) {
      if (game.getTeamA() == null && index < shuffled.size()) {
        game.setTeamA(shuffled.get(index++));
      }
      if (game.getTeamB() == null && index < shuffled.size()) {
        game.setTeamB(shuffled.get(index++));
      }
      if (index >= shuffled.size()) {
        break;
      }
    }
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
    List<Game> games          = round.getGames();
    List<Slot> availableSlots = new ArrayList<>();
    for (Game g : games) {
      if (g.getTeamA() == null) {
        availableSlots.add(new Slot(g, true));
      }
      if (g.getTeamB() == null) {
        availableSlots.add(new Slot(g, false));
      }
    }
    Collections.shuffle(availableSlots);
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
  // @todo this method should manage pools
  public static void placeTeamsInOrder(Round round, List<PlayerPair> teams) {
    if (round == null || round.getGames() == null || teams == null || teams.isEmpty()) {
      return;
    }

    int index = 0;
    for (Game game : round.getGames()) {
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

    if (!round.getPools().isEmpty()) {
      int nbPools        = round.getPools().size();
      int nbPairsPerPool = teams.size() / nbPools;
      for (int poolIndex = 0; poolIndex < nbPairsPerPool; poolIndex++) {
        for (int i = 0; i < nbPairsPerPool; i++) {
          System.out.println("adding player " + (poolIndex * nbPairsPerPool) + i + " to pool " + poolIndex);
          round.getPools().get(poolIndex).addPair(teams.get((poolIndex * nbPairsPerPool) + i));
        }
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
