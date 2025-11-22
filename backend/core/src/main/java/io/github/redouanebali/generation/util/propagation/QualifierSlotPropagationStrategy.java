package io.github.redouanebali.generation.util.propagation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic strategy: propagates winners in non-classic cases. Used when the bracket structure is not strictly binary, for example for qualification
 * rounds, lucky losers, or any round where the number of matches in the next round does not match half of the current round.
 */
@Slf4j
@RequiredArgsConstructor
public class QualifierSlotPropagationStrategy implements PropagationStrategy {

  // Static cache shared across all instances to remember qualifier positions across propagation calls
  // Key: tournament hashcode + gameIdx + sideA -> qualifier number
  private static final java.util.Map<String, Integer>        GLOBAL_QUALIFIER_POSITION_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
  private final        WinnerPropagationUtil                 util;
  // Instance cache: built once per propagation pass
  private              java.util.Map<Integer, QualifierSlot> positionCache                   = null;

  /**
   * Clear the global qualifier position cache. This is useful for testing scenarios that simulate DB reload (where Game instances are recreated). In
   * production, this should rarely be needed as the cache auto-updates when fresh qualifiers are found.
   */
  public static void clearGlobalCache() {
    GLOBAL_QUALIFIER_POSITION_CACHE.clear();
    log.debug("[QualifierSlotPropagationStrategy] Cleared global qualifier position cache");
  }

  /**
   * Reset instance cache for new propagation pass. Global cache is NOT cleared - it's updated automatically when fresh qualifiers are found.
   */
  public void resetCache() {
    positionCache = null;
    log.debug("[QualifierSlotPropagationStrategy] Reset instance cache");
  }

  @Override
  public boolean placeWinner(List<Game> nextGames, int currentGameIndex, PlayerPair winner) {
    // Build position cache on first call
    if (positionCache == null) {
      positionCache = buildPositionCache(nextGames);
    }

    int           targetQualifierNumber = currentGameIndex + 1;
    QualifierSlot targetSlot            = positionCache.get(targetQualifierNumber);

    if (targetSlot == null) {
      if (winner != null) {
        log.warn("[QualifierSlotPropagationStrategy] Q{} slot not found in position cache, cannot place winner", targetQualifierNumber);
      }
      return false;
    }

    // Save position to global cache for future calls
    String cacheKey = buildCacheKey(nextGames, targetSlot.gameIndex, targetSlot.isTeamA);
    GLOBAL_QUALIFIER_POSITION_CACHE.put(cacheKey, targetQualifierNumber);

    Game targetGame = nextGames.get(targetSlot.gameIndex);

    // Si winner est null, garder le slot tel quel (ne rien faire)
    if (winner == null) {
      log.debug("[QualifierSlotPropagationStrategy] No winner for match {}, keeping Q{} slot as is",
                currentGameIndex, targetQualifierNumber);
      return true;
    }

    // Place the winner (either in the QUALIFIER placeholder or replacing the previous winner)
    log.debug("[QualifierSlotPropagationStrategy] Placing winner (seed {}) in Q{} slot -> Game[{}].{}",
              winner.getSeed(), targetQualifierNumber, targetSlot.gameIndex, targetSlot.isTeamA ? "TeamA" : "TeamB");
    if (targetSlot.isTeamA) {
      targetGame.setTeamA(winner);
    } else {
      targetGame.setTeamB(winner);
    }
    return true;
  }

  /**
   * Build position cache: ALWAYS scan for current qualifier placeholders first (highest priority), then use global cache as fallback for positions
   * where qualifiers have been replaced. This ensures we use fresh positions when qualifiers are present.
   */
  private java.util.Map<Integer, QualifierSlot> buildPositionCache(List<Game> nextGames) {
    java.util.Map<Integer, QualifierSlot> cache = new java.util.HashMap<>();

    // FIRST: Scan for all current qualifier placeholders (HIGHEST PRIORITY)
    // This ensures we always use fresh positions when qualifiers are present
    for (int gameIdx = 0; gameIdx < nextGames.size(); gameIdx++) {
      Game game = nextGames.get(gameIdx);

      // Check teamA
      if (game.getTeamA() != null && game.getTeamA().isQualifier()) {
        Integer qualNum = game.getTeamA().getQualifierIndex();
        if (qualNum != null) {
          cache.put(qualNum, new QualifierSlot(gameIdx, true));
          // Update global cache with this fresh position
          String key = buildCacheKey(nextGames, gameIdx, true);
          GLOBAL_QUALIFIER_POSITION_CACHE.put(key, qualNum);
          log.debug("[QualifierSlotPropagationStrategy] Found fresh Q{} at Game[{}].teamA", qualNum, gameIdx);
        }
      }

      // Check teamB
      if (game.getTeamB() != null && game.getTeamB().isQualifier()) {
        Integer qualNum = game.getTeamB().getQualifierIndex();
        if (qualNum != null) {
          cache.put(qualNum, new QualifierSlot(gameIdx, false));
          // Update global cache with this fresh position
          String key = buildCacheKey(nextGames, gameIdx, false);
          GLOBAL_QUALIFIER_POSITION_CACHE.put(key, qualNum);
          log.debug("[QualifierSlotPropagationStrategy] Found fresh Q{} at Game[{}].teamB", qualNum, gameIdx);
        }
      }
    }

    // SECOND: For qualifiers not found above (already replaced), restore from global cache
    for (int gameIdx = 0; gameIdx < nextGames.size(); gameIdx++) {
      String  keyA     = buildCacheKey(nextGames, gameIdx, true);
      Integer qualNumA = GLOBAL_QUALIFIER_POSITION_CACHE.get(keyA);
      if (qualNumA != null && !cache.containsKey(qualNumA)) {
        cache.put(qualNumA, new QualifierSlot(gameIdx, true));
        log.debug("[QualifierSlotPropagationStrategy] Restored Q{} from cache at Game[{}].teamA", qualNumA, gameIdx);
      }

      String  keyB     = buildCacheKey(nextGames, gameIdx, false);
      Integer qualNumB = GLOBAL_QUALIFIER_POSITION_CACHE.get(keyB);
      if (qualNumB != null && !cache.containsKey(qualNumB)) {
        cache.put(qualNumB, new QualifierSlot(gameIdx, false));
        log.debug("[QualifierSlotPropagationStrategy] Restored Q{} from cache at Game[{}].teamB", qualNumB, gameIdx);
      }
    }

    log.debug("[QualifierSlotPropagationStrategy] Built position cache with {} qualifier positions (global cache size: {})",
              cache.size(), GLOBAL_QUALIFIER_POSITION_CACHE.size());
    return cache;
  }

  /**
   * Build a unique cache key for a slot position. Uses game IDs if available (persisted games) to create stable keys across DB reloads. Falls back to
   * identityHashCode for in-memory games (tests).
   */
  private String buildCacheKey(List<Game> nextGames, int gameIdx, boolean isTeamA) {
    if (gameIdx >= nextGames.size()) {
      return "unknown_" + gameIdx + "_" + (isTeamA ? "A" : "B");
    }

    Game game = nextGames.get(gameIdx);

    // If game has an ID (persisted), use it for stable cache key across DB reloads
    if (game.getId() != null) {
      return "game_" + game.getId() + "_" + (isTeamA ? "A" : "B");
    }

    // Fallback for non-persisted games (tests): use identity-based key
    return "mem_" + System.identityHashCode(nextGames) + "_" + gameIdx + "_" + (isTeamA ? "A" : "B");
  }

  /**
   * Represents a QUALIFIER slot in a game of the next round
   */
  private record QualifierSlot(int gameIndex, boolean isTeamA) {

  }
}

