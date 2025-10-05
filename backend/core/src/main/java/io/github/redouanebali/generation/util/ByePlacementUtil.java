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
   * Places BYE teams in a round, skipping slots reserved for qualifiers.
   *
   * @param round the round to place BYEs in
   * @param totalPairs number of direct-entry teams
   * @param nbSeeds number of seeds
   * @param drawSize total draw size
   * @param nbQualifiers number of qualifier slots to skip
   */
  public static void placeByeTeams(Round round, int totalPairs, int nbSeeds, int drawSize, int nbQualifiers) {
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

    final List<Game> games = round.getGames();
    final int        slots = games.size() * 2;
    if (slots != drawSize) {
      throw new IllegalStateException("Round games do not match drawSize: games*2=" + slots + ", drawSize=" + drawSize);
    }

    int existingByes = countExistingByes(games);
    int byesToPlace  = Math.max(0, drawSize - totalPairs - nbQualifiers - existingByes);
    if (byesToPlace == 0) {
      return;
    }

    // 1. Place BYEs opposite seeds, skipping qualifier slots
    List<Integer> seedSlots = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);
    int           placed    = 0;
    for (int slot : seedSlots) {
      if (placed >= byesToPlace) {
        break;
      }
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);
      Game    g         = games.get(gameIndex);
      if (left) {
        if (g.getTeamB() == null && !isReservedForQualifier(g, false)) {
          g.setTeamB(PlayerPair.bye());
          placed++;
        }
      } else {
        if (g.getTeamA() == null && !isReservedForQualifier(g, true)) {
          g.setTeamA(PlayerPair.bye());
          placed++;
        }
      }
    }
    // 2. Fallback: place BYEs in any available slot not reserved for qualifier
    for (Game g : games) {
      if (placed >= byesToPlace) {
        break;
      }
      if (g.getTeamA() == null && !isReservedForQualifier(g, true)) {
        g.setTeamA(PlayerPair.bye());
        placed++;
      }
      if (placed >= byesToPlace) {
        break;
      }
      if (g.getTeamB() == null && !isReservedForQualifier(g, false)) {
        g.setTeamB(PlayerPair.bye());
        placed++;
      }
    }
    // 3. Last resort: throw if not enough slots
    if (placed < byesToPlace) {
      throw new IllegalStateException("Not enough empty slots to place all BYEs: remaining=" + (byesToPlace - placed));
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
