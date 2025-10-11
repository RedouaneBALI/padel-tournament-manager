package io.github.redouanebali.generation.util;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import java.util.List;

/**
 * Utility class for distributing teams across pools. Centralizes round-robin and other distribution strategies.
 */
public class PoolDistributionUtil {

  private PoolDistributionUtil() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Distributes teams across pools using simple round-robin distribution. Team i goes to pool (i % nbPools).
   *
   * @param pools the pools to distribute teams into
   * @param teams the teams to distribute
   */
  public static void distributeRoundRobin(List<Pool> pools, List<PlayerPair> teams) {
    if (pools == null || pools.isEmpty() || teams == null || teams.isEmpty()) {
      return;
    }

    int nbPools = pools.size();
    for (int i = 0; i < teams.size(); i++) {
      int poolIndex = i % nbPools;
      pools.get(poolIndex).addPair(teams.get(i));
    }
  }

  /**
   * Distributes teams across pools using round-robin with capacity check. If a pool is full, tries the next available pool.
   *
   * @param pools the pools to distribute teams into
   * @param teams the teams to distribute
   * @param maxPairsPerPool maximum number of pairs per pool
   * @return the number of teams that couldn't be placed
   */
  public static int distributeRoundRobinWithCapacity(List<Pool> pools, List<PlayerPair> teams, int maxPairsPerPool) {
    if (pools == null || pools.isEmpty() || teams == null || teams.isEmpty()) {
      return 0;
    }

    int nbPools  = pools.size();
    int unplaced = 0;

    for (int i = 0; i < teams.size(); i++) {
      PlayerPair team      = teams.get(i);
      int        poolIndex = i % nbPools;

      boolean placed = tryPlaceInPool(pools, team, poolIndex, maxPairsPerPool, nbPools);
      if (!placed) {
        unplaced++;
      }
    }

    return unplaced;
  }

  /**
   * Tries to place a team in the target pool or finds the next available pool.
   *
   * @param pools the list of pools
   * @param team the team to place
   * @param startPoolIndex the preferred pool index
   * @param maxPairsPerPool maximum capacity per pool
   * @param nbPools total number of pools
   * @return true if the team was placed, false otherwise
   */
  private static boolean tryPlaceInPool(List<Pool> pools, PlayerPair team, int startPoolIndex, int maxPairsPerPool, int nbPools) {
    Pool targetPool = pools.get(startPoolIndex);

    if (targetPool.getPairs().size() < maxPairsPerPool) {
      targetPool.addPair(team);
      return true;
    }

    return findAndPlaceInNextAvailablePool(pools, team, startPoolIndex, maxPairsPerPool, nbPools);
  }

  /**
   * Finds the next available pool with capacity and places the team.
   *
   * @param pools the list of pools
   * @param team the team to place
   * @param startPoolIndex the index to start searching from
   * @param maxPairsPerPool maximum capacity per pool
   * @param nbPools total number of pools
   * @return true if a pool was found and team placed, false otherwise
   */
  private static boolean findAndPlaceInNextAvailablePool(List<Pool> pools, PlayerPair team, int startPoolIndex, int maxPairsPerPool, int nbPools) {
    for (int j = 1; j < nbPools; j++) {
      int  nextPoolIndex = (startPoolIndex + j) % nbPools;
      Pool nextPool      = pools.get(nextPoolIndex);
      if (nextPool.getPairs().size() < maxPairsPerPool) {
        nextPool.addPair(team);
        return true;
      }
    }
    return false;
  }

  /**
   * Distributes teams across pools using snake/serpentine distribution. Snake order for 4 pools: [0, 3, 2, 1, 0, 3, 2, 1, ...] This ensures more
   * balanced distribution when seeds matter.
   *
   * @param pools the pools to distribute teams into
   * @param teams the teams to distribute
   */
  public static void distributeSnake(List<Pool> pools, List<PlayerPair> teams) {
    if (pools == null || pools.isEmpty() || teams == null || teams.isEmpty()) {
      return;
    }

    int           nbPools    = pools.size();
    List<Integer> snakeOrder = createSnakeOrder(nbPools);

    for (int i = 0; i < teams.size(); i++) {
      int poolIndex = snakeOrder.get(i % snakeOrder.size());
      pools.get(poolIndex).addPair(teams.get(i));
    }
  }

  /**
   * Creates snake order pattern: [0, n-1, n-2, ..., 1] For 4 pools: [0, 3, 2, 1] For 3 pools: [0, 2, 1]
   *
   * @param nbPools number of pools
   * @return list of pool indices in snake order
   */
  private static List<Integer> createSnakeOrder(int nbPools) {
    List<Integer> snakeOrder = new java.util.ArrayList<>();
    snakeOrder.add(0);
    for (int i = nbPools - 1; i >= 1; i--) {
      snakeOrder.add(i);
    }
    return snakeOrder;
  }

  /**
   * Distributes teams starting from a specific pool index and moving sequentially. Used for manual distribution or specific placement strategies.
   *
   * @param pools the pools to distribute teams into
   * @param teams the teams to distribute
   * @param startPoolIndex the pool index to start from
   */
  public static void distributeSequential(List<Pool> pools, List<PlayerPair> teams, int startPoolIndex) {
    if (pools == null || pools.isEmpty() || teams == null || teams.isEmpty()) {
      return;
    }

    int nbPools   = pools.size();
    int poolIndex = startPoolIndex % nbPools;

    for (PlayerPair team : teams) {
      pools.get(poolIndex).addPair(team);
      poolIndex = (poolIndex + 1) % nbPools;
    }
  }
}
