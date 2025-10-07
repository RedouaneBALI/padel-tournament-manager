package io.github.redouanebali.generation.util;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import java.util.List;

/**
 * Utility for placing BYE teams in tournaments. Extracted logic from KnockoutPhase to make it reusable.
 */
public class ByePlacementUtil {

  /**
   * Places BYE teams in a round based on the total number of pairs.
   */
  public static void placeByeTeams(Round round, int totalPairs, int nbSeeds, int drawSize) {
    placeByeTeams(round, totalPairs, nbSeeds, drawSize, 0);
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
    validateInputParameters(round, totalPairs, drawSize);

    final List<Game> games = round.getGames();
    validateGamesStructure(games, drawSize);

    int byesToPlace = calculateByesToPlace(games, totalPairs, nbQualifiers, drawSize);
    if (byesToPlace == 0) {
      return;
    }

    placeByesInRound(games, byesToPlace, nbSeeds, drawSize);
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
  private static void placeByesInRound(List<Game> games, int byesToPlace, int nbSeeds, int drawSize) {
    int placed = 0;

    // Strategy 1: Place BYEs opposite seeds
    placed += placeByesOppositeSeedsStrategy(games, byesToPlace, nbSeeds, drawSize, placed);

    // Strategy 2: Fallback placement in any available slot
    placed += placeByesFallbackStrategy(games, byesToPlace, placed);

    // Validate all BYEs were placed
    validateAllByesPlaced(byesToPlace, placed);
  }

  /**
   * Places BYEs opposite seed positions.
   */
  private static int placeByesOppositeSeedsStrategy(List<Game> games, int byesToPlace, int nbSeeds, int drawSize, int alreadyPlaced) {
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
  private static int placeByesFallbackStrategy(List<Game> games, int byesToPlace, int alreadyPlaced) {
    int placed = alreadyPlaced;

    for (Game g : games) {
      if (placed >= byesToPlace) {
        break;
      }

      placed += tryPlaceByeInGame(g, byesToPlace, placed);
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

    int     gameIndex  = seedSlot / 2;
    boolean isLeftSlot = (seedSlot % 2 == 0);
    Game    game       = games.get(gameIndex);

    if (isLeftSlot) {
      return tryPlaceByeInTeamPosition(game, false) ? 1 : 0; // Place in TeamB (opposite)
    } else {
      return tryPlaceByeInTeamPosition(game, true) ? 1 : 0;  // Place in TeamA (opposite)
    }
  }

  /**
   * Tries to place BYEs in both positions of a game.
   */
  private static int tryPlaceByeInGame(Game game, int byesToPlace, int alreadyPlaced) {
    int placed = 0;

    if (alreadyPlaced + placed < byesToPlace && tryPlaceByeInTeamPosition(game, true)) {
      placed++;
    }

    if (alreadyPlaced + placed < byesToPlace && tryPlaceByeInTeamPosition(game, false)) {
      placed++;
    }

    return placed;
  }

  /**
   * Tries to place a BYE in a specific team position.
   */
  private static boolean tryPlaceByeInTeamPosition(Game game, boolean isTeamA) {
    if (isTeamA) {
      if (game.getTeamA() == null && !isReservedForQualifier(game, true)) {
        game.setTeamA(PlayerPair.bye());
        return true;
      }
    } else {
      if (game.getTeamB() == null && !isReservedForQualifier(game, false)) {
        game.setTeamB(PlayerPair.bye());
        return true;
      }
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
    // By default, always false. Main draw logic should ensure qualifiers are placed before BYEs.
    return false;
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

  /**
   * Places BYEs opposite protected teams.
   */
  private static int placeByesOppositeProtectedTeams(List<Game> games, List<Integer> protectedSlots, int byesToPlace) {
    for (int i = 0; i < protectedSlots.size() && byesToPlace > 0; i++) {
      int     slot      = protectedSlots.get(i);
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);
      Game    g         = games.get(gameIndex);

      // Place BYE at opposite position if empty
      if (left) {
        // Protected slot is on the left (TeamA), place BYE on the right (TeamB) if empty
        if (g.getTeamB() == null) {
          g.setTeamB(PlayerPair.bye());
          byesToPlace--;
        }
      } else {
        // Protected slot is on the right (TeamB), place BYE on the left (TeamA) if empty
        if (g.getTeamA() == null) {
          g.setTeamA(PlayerPair.bye());
          byesToPlace--;
        }
      }
    }
    return byesToPlace;
  }

  /**
   * Places remaining BYEs using fallback logic.
   */
  private static int placeFallbackByes(List<Game> games, int byesToPlace) {
    for (Game g : games) {
      if (byesToPlace == 0) {
        break;
      }

      boolean aEmpty = (g.getTeamA() == null);
      boolean bEmpty = (g.getTeamB() == null);

      if (aEmpty ^ bEmpty) { // exactly one side empty
        if (aEmpty) {
          g.setTeamA(PlayerPair.bye());
        } else {
          g.setTeamB(PlayerPair.bye());
        }
        byesToPlace--;
      }
    }
    return byesToPlace;
  }

  /**
   * Places BYEs as a last resort (BYE vs BYE if necessary).
   */
  private static void placeLastResortByes(List<Game> games, int byesToPlace) {
    for (Game g : games) {
      if (byesToPlace == 0) {
        break;
      }

      if (g.getTeamA() == null) {
        g.setTeamA(PlayerPair.bye());
        byesToPlace--;
        if (byesToPlace == 0) {
          break;
        }
      }

      if (g.getTeamB() == null) {
        g.setTeamB(PlayerPair.bye());
        byesToPlace--;
      }
    }

    if (byesToPlace > 0) {
      throw new IllegalStateException("Not enough empty slots to place all BYEs: remaining=" + byesToPlace);
    }
  }
}
