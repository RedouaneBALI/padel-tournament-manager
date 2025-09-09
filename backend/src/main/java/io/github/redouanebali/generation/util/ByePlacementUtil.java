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

    // Count how many BYEs are already present to avoid placing too many
    int existingByes = countExistingByes(games);
    int byesToPlace  = Math.max(0, drawSize - totalPairs - existingByes);

    if (byesToPlace == 0) {
      return;
    }

    // Get seed positions for teams to protect with BYEs
    List<Integer> protectedSlots = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);

    // Place BYEs opposite protected teams (TS1, TS2, ..., TS_n)
    byesToPlace = placeByesOppositeProtectedTeams(games, protectedSlots, byesToPlace);

    // If there are still BYEs to place, use fallback logic
    if (byesToPlace > 0) {
      byesToPlace = placeFallbackByes(games, byesToPlace);
    }

    // As a last resort, allow BYE vs BYE if still necessary
    if (byesToPlace > 0) {
      placeLastResortByes(games, byesToPlace);
    }
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
