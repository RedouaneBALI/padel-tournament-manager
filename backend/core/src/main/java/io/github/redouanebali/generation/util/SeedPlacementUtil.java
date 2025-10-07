package io.github.redouanebali.generation.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.TeamSide;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Utility for placing seeded teams in tournaments. Extracted logic from KnockoutPhase to make it reusable.
 */
public class SeedPlacementUtil {

  /**
   * Places seeded teams in their theoretical positions in a round.
   *
   * @param round the round to place teams in
   * @param playerPairs the list of player pairs (should be sorted by seed)
   * @param nbSeeds number of seeds to place
   * @param drawSize the size of the draw (must match actual slots)
   * @throws IllegalArgumentException if parameters are invalid
   * @throws IllegalStateException if slots are already occupied or drawSize mismatch
   */
  public static void placeSeedTeams(Round round, List<PlayerPair> playerPairs, int nbSeeds, int drawSize) {
    if (!isValidSeedPlacementParams(round, playerPairs, nbSeeds, drawSize)) {
      return;
    }

    final List<Game> games = round.getGames();
    final int        slots = games.size() * 2;
    validateDrawSize(drawSize, slots);

    int byesToPlace                 = Math.max(0, drawSize - playerPairs.size());
    int teamsToPlaceAtSeedPositions = calculateTeamsToPlace(nbSeeds, byesToPlace, playerPairs.size());
    if (teamsToPlaceAtSeedPositions == 0) {
      return;
    }

    List<Integer>    seedPositions = getSeedPositions(drawSize, nbSeeds, teamsToPlaceAtSeedPositions);
    List<PlayerPair> teamsToPlace  = playerPairs.subList(0, teamsToPlaceAtSeedPositions);
    placeTeamsAtSlots(games, teamsToPlace, seedPositions, false);
  }

  private static boolean isValidSeedPlacementParams(Round round, List<PlayerPair> playerPairs, int nbSeeds, int drawSize) {
    if (round == null || round.getGames() == null || playerPairs == null || nbSeeds == 0) {
      return false;
    }
    if (nbSeeds < 0) {
      throw new IllegalArgumentException("nbSeeds cannot be negative");
    }
    if (drawSize < 0) {
      throw new IllegalArgumentException("drawSize cannot be negative");
    }
    return true;
  }

  private static void validateDrawSize(int drawSize, int slots) {
    if (drawSize != 0 && drawSize != slots) {
      throw new IllegalStateException("Configured drawSize=" + drawSize + " but games provide slots=" + slots);
    }
  }

  private static int calculateTeamsToPlace(int nbSeeds, int byesToPlace, int totalTeams) {
    int teamsToPlace = Math.max(nbSeeds, byesToPlace);
    return Math.min(teamsToPlace, totalTeams);
  }

  private static List<Integer> getSeedPositions(int drawSize, int nbSeeds, int teamsToPlaceAtSeedPositions) {
    List<Integer> allPositions = loadSeedPositionsFromJson(drawSize, nbSeeds);
    int           max          = Math.min(teamsToPlaceAtSeedPositions, allPositions.size());
    return allPositions.subList(0, max);
  }

  /**
   * Gets seed positions from JSON file.
   */
  public static List<Integer> getSeedsPositions(int drawSize, int nbSeeds) {
    if (nbSeeds == 0) {
      return new ArrayList<>();
    }

    return loadSeedPositionsFromJson(drawSize, nbSeeds);
  }

  /**
   * Public comparator for sorting pairs by seed.
   */
  public static Comparator<PlayerPair> getSeedComparator() {
    return (pair1, pair2) -> {
      int seed1 = pair1.getSeed();
      int seed2 = pair2.getSeed();
      if (seed1 > 0 && seed2 > 0) {
        return Integer.compare(seed1, seed2);
      }
      if (seed1 > 0) {
        return -1;
      }
      if (seed2 > 0) {
        return 1;
      }
      return 0;
    };
  }

  /**
   * Place a list of teams at the given slots in the games list. If allowQualifierOverwrite is true, will overwrite QUALIFIER placeholders.
   */
  public static void placeTeamsAtSlots(List<Game> games, List<PlayerPair> teams, List<Integer> slots, boolean allowQualifierOverwrite) {
    for (int i = 0; i < teams.size() && i < slots.size(); i++) {
      int        slot      = slots.get(i);
      int        gameIndex = slot / 2;
      TeamSide   side      = (slot % 2 == 0) ? TeamSide.TEAM_A : TeamSide.TEAM_B;
      Game       g         = games.get(gameIndex);
      PlayerPair team      = teams.get(i);
      placeTeamInGameSlot(g, team, side, allowQualifierOverwrite, gameIndex);
    }
  }

  private static void placeTeamInGameSlot(Game g, PlayerPair team, TeamSide side, boolean allowQualifierOverwrite, int gameIndex) {
    if (side == TeamSide.TEAM_A) {
      if (canPlaceTeam(g.getTeamA(), allowQualifierOverwrite)) {
        g.setTeamA(team);
      } else {
        throw new IllegalStateException("Seed slot already occupied: game=" + gameIndex + ", side=TEAM_A");
      }
    } else {
      if (canPlaceTeam(g.getTeamB(), allowQualifierOverwrite)) {
        g.setTeamB(team);
      } else {
        throw new IllegalStateException("Seed slot already occupied: game=" + gameIndex + ", side=TEAM_B");
      }
    }
  }

  private static boolean canPlaceTeam(PlayerPair current, boolean allowQualifierOverwrite) {
    return current == null || (allowQualifierOverwrite && current.getType() == io.github.redouanebali.model.PairType.QUALIFIER);
  }

  /**
   * Loads seed positions from JSON file.
   */
  private static List<Integer> loadSeedPositionsFromJson(int drawSize, int nbSeeds) {
    try {
      JsonNode      nbSeedsNode  = loadAndNavigateToSeedNode(drawSize, nbSeeds);
      List<String>  sortedFields = getSortedFieldNames(nbSeedsNode);
      List<Integer> positions    = processAllSeedGroups(nbSeedsNode, sortedFields);
      return positions.subList(0, Math.min(nbSeeds, positions.size()));
    } catch (Exception e) {
      throw new RuntimeException("Failed to load seed positions from JSON", e);
    }
  }

  private static JsonNode loadAndNavigateToSeedNode(int drawSize, int nbSeeds) throws Exception {
    InputStream inputStream = SeedPlacementUtil.class.getResourceAsStream("/seed_positions.json");
    if (inputStream == null) {
      throw new IllegalStateException("seed_positions.json not found in resources");
    }

    ObjectMapper mapper       = new ObjectMapper();
    JsonNode     rootNode     = mapper.readTree(inputStream);
    JsonNode     drawSizeNode = rootNode.get(String.valueOf(drawSize));

    if (drawSizeNode == null) {
      throw new IllegalArgumentException("DrawSize " + drawSize + " not supported in seed_positions.json");
    }

    int      nbSeedsToUse = calculateNbSeedsToUse(nbSeeds, drawSize);
    JsonNode nbSeedsNode  = drawSizeNode.get(String.valueOf(nbSeedsToUse));

    if (nbSeedsNode == null) {
      throw new IllegalArgumentException("NbSeeds " + nbSeedsToUse + " not supported for drawSize " + drawSize);
    }

    return nbSeedsNode;
  }

  private static int calculateNbSeedsToUse(int nbSeeds, int drawSize) {
    if (nbSeeds <= 0 || (nbSeeds & (nbSeeds - 1)) == 0) {
      return nbSeeds;
    }
    int nbSeedsToUse = Integer.highestOneBit(nbSeeds) << 1;
    return Math.min(nbSeedsToUse, drawSize);
  }

  private static List<String> getSortedFieldNames(JsonNode nbSeedsNode) {
    Iterator<String> fieldNames   = nbSeedsNode.fieldNames();
    List<String>     sortedFields = new ArrayList<>();
    fieldNames.forEachRemaining(sortedFields::add);
    sortedFields.sort(SeedPlacementUtil::compareSeedGroupKeys);
    return sortedFields;
  }

  private static int compareSeedGroupKeys(String a, String b) {
    if (a.equals("TS1")) {
      return -1;
    }
    if (b.equals("TS1")) {
      return 1;
    }
    if (a.equals("TS2")) {
      return -1;
    }
    if (b.equals("TS2")) {
      return 1;
    }
    return a.compareTo(b);
  }

  private static List<Integer> processAllSeedGroups(JsonNode nbSeedsNode, List<String> sortedFields) {
    List<Integer> positions = new ArrayList<>();
    for (String groupKey : sortedFields) {
      List<Integer> groupPositions = extractGroupPositions(nbSeedsNode.get(groupKey));
      addGroupPositionsToList(positions, groupPositions, groupKey);
    }
    return positions;
  }

  private static List<Integer> extractGroupPositions(JsonNode positionsArray) {
    List<Integer> groupPositions = new ArrayList<>();
    for (JsonNode posNode : positionsArray) {
      groupPositions.add(posNode.asInt());
    }
    return groupPositions;
  }

  private static void addGroupPositionsToList(List<Integer> positions, List<Integer> groupPositions, String groupKey) {
    if ("TS1".equals(groupKey) || "TS2".equals(groupKey)) {
      positions.addAll(groupPositions);
    } else {
      Collections.shuffle(groupPositions);
      positions.addAll(groupPositions);
    }
  }


  /**
   * Places seeded teams in pools for group phase tournaments. Distributes seeds evenly across pools using round-robin distribution.
   *
   * Examples: - 4 pools, 4 seeds → 1 seed per pool (TS1 in Pool A, TS2 in Pool B, etc.) - 4 pools, 8 seeds → 2 seeds per pool (TS1+TS5 in Pool A,
   * TS2+TS6 in Pool B, etc.)
   *
   * @param round the round containing pools
   * @param playerPairs the list of player pairs
   * @param nbPairsPerPool maximum pairs per pool
   */
  public static void placeSeedTeamsInPools(Round round, List<PlayerPair> playerPairs, int nbPairsPerPool) {
    if (round == null || playerPairs == null || round.getPools().isEmpty()) {
      return;
    }

    // Get seeded teams and sort them by seed number
    List<PlayerPair> seededTeams = playerPairs.stream()
                                              .filter(pair -> pair.getSeed() > 0)
                                              .sorted(getSeedComparator())
                                              .toList();

    if (seededTeams.isEmpty()) {
      return;
    }

    List<Pool> pools   = round.getPools();
    int        nbPools = pools.size();

    // Distribute seeds using round-robin approach
    // This ensures even distribution: seed i goes to pool (i % nbPools)
    for (int i = 0; i < seededTeams.size(); i++) {
      PlayerPair seed       = seededTeams.get(i);
      int        poolIndex  = i % nbPools; // Round-robin distribution
      Pool       targetPool = pools.get(poolIndex);

      // Check if pool has space
      if (targetPool.getPairs().size() < nbPairsPerPool) {
        targetPool.addPair(seed);
      } else {
        // If the target pool is full, find the next available pool
        boolean placed = false;
        for (int j = 0; j < nbPools && !placed; j++) {
          int  nextPoolIndex = (poolIndex + j) % nbPools;
          Pool nextPool      = pools.get(nextPoolIndex);
          if (nextPool.getPairs().size() < nbPairsPerPool) {
            nextPool.addPair(seed);
            placed = true;
          }
        }
        // If no pool has space, the seed cannot be placed (should not happen in normal cases)
      }
    }
  }

  /**
   * Place les seeds dans les pools selon la logique snake officielle. Retourne une liste de pools (List<List<PlayerPair>>) avec les seeds placées.
   */
  public static java.util.List<java.util.List<io.github.redouanebali.model.PlayerPair>> placeSeedsInPoolsSnake(java.util.List<io.github.redouanebali.model.PlayerPair> seeds,
                                                                                                               int nbPools) {
    java.util.List<java.util.List<io.github.redouanebali.model.PlayerPair>> pools = new java.util.ArrayList<>();
    for (int i = 0; i < nbPools; i++) {
      pools.add(new java.util.ArrayList<>());
    }
    // Correction : ordre snake = [0, n-1, n-2, ..., 1]
    java.util.List<Integer> snakeOrder = new java.util.ArrayList<>();
    snakeOrder.add(0);
    for (int i = nbPools - 1; i >= 1; i--) {
      snakeOrder.add(i);
    }
    for (int i = 0; i < seeds.size(); i++) {
      int poolIdx = snakeOrder.get(i % snakeOrder.size());
      pools.get(poolIdx).add(seeds.get(i));
    }
    return pools;
  }
}
