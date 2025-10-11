package io.github.redouanebali.generation.util;

import io.github.redouanebali.generation.util.GameSlotUtil.TeamSlot;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.TeamSide;
import java.util.List;

/**
 * Utility for placing BYE teams in tournaments. Extracted logic from KnockoutPhase to make it reusable.
 */
public class ByePlacementUtil {

  private ByePlacementUtil() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Places BYE teams in a round based on the total number of pairs.
   */
  public static void placeByeTeams(Round round, int totalPairs, int nbSeeds, int drawSize) {
    placeByeTeams(round, totalPairs, nbSeeds, drawSize, 0, false);
  }

  /**
   * Places BYE teams in the round based on the number of actual pairs and seeds. Skips qualifier slots when nbQualifiers > 0.
   *
   * @param round the round to place BYEs in
   * @param totalPairs total number of real pairs
   * @param nbSeeds number of seeds
   * @param drawSize total draw size
   * @param nbQualifiers number of qualifier slots to skip
   */
  public static void placeByeTeams(Round round, int totalPairs, int nbSeeds, int drawSize, int nbQualifiers) {
    placeByeTeams(round, totalPairs, nbSeeds, drawSize, nbQualifiers, false);
  }

  /**
   * Places BYE teams in the round based on the number of actual pairs and seeds. Skips qualifier slots when nbQualifiers > 0.
   *
   * @param round the round to place BYEs in
   * @param totalPairs total number of real pairs
   * @param nbSeeds number of seeds
   * @param drawSize total draw size
   * @param nbQualifiers number of qualifier slots to skip
   * @param allowByeVsBye whether to allow BYE vs BYE matches (true for staggered entry mode)
   */
  public static void placeByeTeams(Round round, int totalPairs, int nbSeeds, int drawSize, int nbQualifiers, boolean allowByeVsBye) {
    validateInputParameters(round, totalPairs, drawSize);

    final List<Game> games = round.getGames();
    validateGamesStructure(games, drawSize);

    int byesToPlace = calculateByesToPlace(games, totalPairs, nbQualifiers, drawSize);
    if (byesToPlace == 0) {
      return;
    }

    placeByesInRound(games, byesToPlace, nbSeeds, drawSize, allowByeVsBye);
  }

  /**
   * Validates input parameters for bye placement.
   */
  private static void validateInputParameters(Round round, int totalPairs, int drawSize) {
    if (round == null) {
      throw new IllegalArgumentException("round must not be null");
    }
    if (round.getGames() == null) {
      throw new IllegalArgumentException("round/games must not be null");
    }
    if (round.getGames().isEmpty()) {
      throw new IllegalArgumentException("round/games must not be empty");
    }
    if (drawSize <= 0 || (drawSize & (drawSize - 1)) != 0) {
      throw new IllegalArgumentException("drawSize must be a power of two");
    }
    if (totalPairs > drawSize) {
      throw new IllegalArgumentException("totalPairs cannot exceed drawSize");
    }
  }

  /**
   * Validates that games structure matches expected draw size.
   */
  private static void validateGamesStructure(List<Game> games, int drawSize) {
    final int slots = games.size() * 2;
    if (slots != drawSize) {
      throw new IllegalStateException("Round games do not match drawSize: games*2=" + slots + ", drawSize=" + drawSize);
    }
  }

  /**
   * Calculates how many BYEs need to be placed.
   */
  private static int calculateByesToPlace(List<Game> games, int totalPairs, int nbQualifiers, int drawSize) {
    int existingByes = countExistingByes(games);
    return Math.max(0, drawSize - totalPairs - nbQualifiers - existingByes);
  }

  /**
   * Places BYEs in the round using different strategies.
   */
  private static void placeByesInRound(List<Game> games, int byesToPlace, int nbSeeds, int drawSize, boolean allowByeVsBye) {
    int placed = 0;

    // Strategy 1: Place BYEs opposite seeds
    placed += placeByesOppositeSeedsStrategy(games, byesToPlace, nbSeeds, drawSize, placed, allowByeVsBye);

    // Strategy 2: Fallback placement in any available slot
    placed += placeByesFallbackStrategy(games, byesToPlace, placed, allowByeVsBye);

    // Validate all BYEs were placed
    validateAllByesPlaced(byesToPlace, placed);
  }

  /**
   * Places BYEs opposite seed positions.
   */
  private static int placeByesOppositeSeedsStrategy(List<Game> games,
                                                    int byesToPlace,
                                                    int nbSeeds,
                                                    int drawSize,
                                                    int alreadyPlaced,
                                                    boolean allowByeVsBye) {
    List<Integer> seedSlots = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);
    int           placed    = alreadyPlaced;

    for (int slot : seedSlots) {
      if (placed >= byesToPlace) {
        break;
      }
      placed += tryPlaceByeAtOppositeSlot(games, slot, placed < byesToPlace);
    }

    return placed - alreadyPlaced;
  }

  /**
   * Places BYEs in any available slot as fallback.
   */
  private static int placeByesFallbackStrategy(List<Game> games, int byesToPlace, int alreadyPlaced, boolean allowByeVsBye) {
    int placed = alreadyPlaced;

    for (Game g : games) {
      if (placed >= byesToPlace) {
        break;
      }

      placed += tryPlaceByeInGame(g, byesToPlace, placed, allowByeVsBye);
    }

    return placed - alreadyPlaced;
  }

  /**
   * Tries to place a BYE at the opposite slot of a seed position.
   */
  private static int tryPlaceByeAtOppositeSlot(List<Game> games, int seedSlot, boolean canPlace) {
    if (!canPlace) {
      return 0;
    }

    int      oppositeSlot = GameSlotUtil.getOppositeSlot(seedSlot);
    TeamSlot teamSlot     = GameSlotUtil.getTeamSlot(oppositeSlot);
    Game     game         = games.get(teamSlot.gameIndex());

    return GameSlotUtil.tryPlaceTeam(game, teamSlot.side(), PlayerPair.bye(), false) ? 1 : 0;
  }

  /**
   * Tries to place BYEs in both positions of a game.
   */
  private static int tryPlaceByeInGame(Game game, int byesToPlace, int alreadyPlaced, boolean allowByeVsBye) {
    int placed = 0;

    if (alreadyPlaced + placed < byesToPlace && tryPlaceByeInTeamPosition(game, true, allowByeVsBye)) {
      placed++;
    }

    if (alreadyPlaced + placed < byesToPlace && tryPlaceByeInTeamPosition(game, false, allowByeVsBye)) {
      placed++;
    }

    return placed;
  }

  /**
   * Tries to place a BYE in a specific team position.
   */
  private static boolean tryPlaceByeInTeamPosition(Game game, boolean isTeamA, boolean allowByeVsBye) {
    TeamSide side = isTeamA ? io.github.redouanebali.model.TeamSide.TEAM_A : io.github.redouanebali.model.TeamSide.TEAM_B;

    // Don't place BYE if slot is reserved for qualifier or already occupied
    if (GameSlotUtil.isSlotEmpty(game, side) && !GameSlotUtil.isReservedForQualifier(game, side)) {
      // Check that the opponent is not already a BYE (unless allowByeVsBye is true for staggered entry)
      if (!allowByeVsBye) {
        PlayerPair opponent = isTeamA ? game.getTeamB() : game.getTeamA();
        if (opponent != null && opponent.isBye()) {
          return false; // Don't place BYE if opponent is already a BYE in classic mode
        }
      }

      GameSlotUtil.setTeam(game, side, PlayerPair.bye());
      return true;
    }
    return false;
  }

  /**
   * Validates that all required BYEs were successfully placed.
   */
  private static void validateAllByesPlaced(int byesToPlace, int actuallyPlaced) {
    if (actuallyPlaced < byesToPlace) {
      throw new IllegalStateException("Not enough empty slots to place all BYEs: remaining=" + (byesToPlace - actuallyPlaced));
    }
  }

  // Helper to check if a slot is reserved for a qualifier (to be implemented in main draw logic)
  private static boolean isReservedForQualifier(Game g, boolean isTeamA) {
    // Deprecated: Use GameSlotUtil.isReservedForQualifier instead
    TeamSide side = isTeamA ? io.github.redouanebali.model.TeamSide.TEAM_A : io.github.redouanebali.model.TeamSide.TEAM_B;
    return GameSlotUtil.isReservedForQualifier(g, side);
  }

  /**
   * Counts the number of BYEs already present in the games.
   */
  private static int countExistingByes(List<Game> games) {
    int count = 0;
    for (Game g : games) {
      if (g.getTeamA() != null && g.getTeamA().isBye()) {
        count++;
      }
      if (g.getTeamB() != null && g.getTeamB().isBye()) {
        count++;
      }
    }
    return count;
  }

}
