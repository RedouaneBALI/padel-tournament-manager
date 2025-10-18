package io.github.redouanebali.generation.util;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.TeamSide;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for game slot operations. Centralizes logic for converting slot indices to game positions and team sides.
 */
public class GameSlotUtil {

  /**
   * Converts a slot index to game index. Slot index is 0-based where even indices are TeamA and odd indices are TeamB.
   *
   * @param slot the slot index (0-based)
   * @return the game index
   */
  public static int getGameIndex(int slot) {
    return slot / 2;
  }

  /**
   * Converts a slot index to team side. Even indices (0, 2, 4...) → TeamA Odd indices (1, 3, 5...) → TeamB
   *
   * @param slot the slot index (0-based)
   * @return the team side
   */
  public static TeamSide getTeamSide(int slot) {
    return (slot % 2 == 0) ? TeamSide.TEAM_A : TeamSide.TEAM_B;
  }

  /**
   * Converts a slot index to a TeamSlot object containing both game index and side.
   *
   * @param slot the slot index (0-based)
   * @return a TeamSlot object
   */
  public static TeamSlot getTeamSlot(int slot) {
    return new TeamSlot(getGameIndex(slot), getTeamSide(slot));
  }

  /**
   * Gets the opposite team side in the same game.
   *
   * @param slot the original slot index
   * @return the opposite slot index
   */
  public static int getOppositeSlot(int slot) {
    return (slot % 2 == 0) ? slot + 1 : slot - 1;
  }

  /**
   * Gets the team at a specific side of a game.
   *
   * @param game the game
   * @param side the team side
   * @return the PlayerPair at that side, or null if empty
   */
  public static PlayerPair getTeam(Game game, TeamSide side) {
    return side == TeamSide.TEAM_A ? game.getTeamA() : game.getTeamB();
  }

  /**
   * Sets a team at a specific side of a game.
   *
   * @param game the game
   * @param side the team side
   * @param team the team to set
   */
  public static void setTeam(Game game, TeamSide side, PlayerPair team) {
    if (side == TeamSide.TEAM_A) {
      game.setTeamA(team);
    } else {
      game.setTeamB(team);
    }
  }

  /**
   * Checks if a slot is available for placement.
   *
   * @param game the game
   * @param side the team side
   * @param allowQualifierOverwrite if true, QUALIFIER placeholders can be overwritten
   * @return true if the slot is available
   */
  public static boolean isSlotAvailable(Game game, TeamSide side, boolean allowQualifierOverwrite) {
    PlayerPair current = getTeam(game, side);
    return current == null || (allowQualifierOverwrite && current.getType() == PairType.QUALIFIER);
  }

  /**
   * Checks if a slot is empty (null). BYEs and QUALIFIERs are NOT considered empty.
   *
   * @param game the game
   * @param side the team side
   * @return true if the slot is null
   */
  public static boolean isSlotEmpty(Game game, TeamSide side) {
    PlayerPair team = getTeam(game, side);
    // A slot is empty only if it's null
    // BYEs and QUALIFIERs are NOT considered empty - they should not be overwritten
    return team == null;
  }

  /**
   * Checks if a slot is available for placing a real team. A slot is available if it's null (empty). BYEs and QUALIFIERs are considered as occupied
   * slots.
   *
   * @param game the game
   * @param side the team side
   * @return true if the slot is available for a real team
   */
  public static boolean isSlotAvailableForTeam(Game game, TeamSide side) {
    // Delegate to isSlotEmpty - same logic
    return isSlotEmpty(game, side);
  }

  /**
   * Checks if a slot is reserved for a qualifier.
   *
   * @param game the game
   * @param side the team side
   * @return true if the slot contains a QUALIFIER placeholder
   */
  public static boolean isReservedForQualifier(Game game, TeamSide side) {
    PlayerPair team = getTeam(game, side);
    return team != null && team.getType() == PairType.QUALIFIER;
  }

  /**
   * Places a team at a specific slot if available.
   *
   * @param game the game
   * @param side the team side
   * @param team the team to place
   * @param allowQualifierOverwrite if true, QUALIFIER placeholders can be overwritten
   * @return true if the team was successfully placed
   */
  public static boolean tryPlaceTeam(Game game, TeamSide side, PlayerPair team, boolean allowQualifierOverwrite) {
    if (isSlotAvailable(game, side, allowQualifierOverwrite)) {
      setTeam(game, side, team);
      return true;
    }
    return false;
  }

  /**
   * Places a team at a specific slot, throwing an exception if not available.
   *
   * @param game the game
   * @param side the team side
   * @param team the team to place
   * @param allowQualifierOverwrite if true, QUALIFIER placeholders can be overwritten
   * @param gameIndex the game index (for error message)
   * @throws IllegalStateException if the slot is already occupied
   */
  public static void placeTeamOrThrow(Game game, TeamSide side, PlayerPair team, boolean allowQualifierOverwrite, int gameIndex) {
    if (!tryPlaceTeam(game, side, team, allowQualifierOverwrite)) {
      throw new IllegalStateException("Slot already occupied: game=" + gameIndex + ", side=" + side);
    }
  }

  /**
   * Collects all empty slots from a list of games.
   *
   * @param games the list of games
   * @return list of TeamSlot objects representing empty positions
   */
  public static List<TeamSlot> collectEmptySlots(List<Game> games) {
    List<TeamSlot> emptySlots = new ArrayList<>();
    for (int gameIndex = 0; gameIndex < games.size(); gameIndex++) {
      Game game = games.get(gameIndex);

      if (isSlotEmpty(game, TeamSide.TEAM_A)) {
        emptySlots.add(new TeamSlot(gameIndex, TeamSide.TEAM_A));
      }
      if (isSlotEmpty(game, TeamSide.TEAM_B)) {
        emptySlots.add(new TeamSlot(gameIndex, TeamSide.TEAM_B));
      }
    }
    return emptySlots;
  }

  /**
   * Counts the number of empty slots in a list of games.
   *
   * @param games the list of games
   * @return the number of empty slots
   */
  public static int countEmptySlots(List<Game> games) {
    int count = 0;
    for (Game game : games) {
      if (isSlotEmpty(game, TeamSide.TEAM_A)) {
        count++;
      }
      if (isSlotEmpty(game, TeamSide.TEAM_B)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Collects all available slots (empty or containing QUALIFIER if allowQualifierOverwrite is true).
   *
   * @param games the list of games
   * @param allowQualifierOverwrite if true, includes slots with QUALIFIER placeholders
   * @return list of TeamSlot objects representing available positions
   */
  public static List<TeamSlot> collectAvailableSlots(List<Game> games, boolean allowQualifierOverwrite) {
    List<TeamSlot> availableSlots = new ArrayList<>();
    for (int gameIndex = 0; gameIndex < games.size(); gameIndex++) {
      Game game = games.get(gameIndex);

      if (isSlotAvailable(game, TeamSide.TEAM_A, allowQualifierOverwrite)) {
        availableSlots.add(new TeamSlot(gameIndex, TeamSide.TEAM_A));
      }
      if (isSlotAvailable(game, TeamSide.TEAM_B, allowQualifierOverwrite)) {
        availableSlots.add(new TeamSlot(gameIndex, TeamSide.TEAM_B));
      }
    }
    return availableSlots;
  }

  /**
   * Places teams in available slots sequentially.
   *
   * @param games the list of games
   * @param teams the teams to place
   * @return the number of teams that couldn't be placed
   */
  public static int placeTeamsSequentially(List<Game> games, List<PlayerPair> teams) {
    int index = 0;

    for (int i = 0; i < games.size() && index < teams.size(); i++) {
      Game game = games.get(i);
      if (isSlotEmpty(game, TeamSide.TEAM_A)) {
        setTeam(game, TeamSide.TEAM_A, teams.get(index++));
      }
      if (index < teams.size() && isSlotEmpty(game, TeamSide.TEAM_B)) {
        setTeam(game, TeamSide.TEAM_B, teams.get(index++));
      }
    }

    return teams.size() - index;
  }

  /**
   * Represents a position in a game (game index + team side).
   */
  public record TeamSlot(int gameIndex, TeamSide side) {

    public boolean isTeamA() {
      return side == TeamSide.TEAM_A;
    }

    public boolean isTeamB() {
      return side == TeamSide.TEAM_B;
    }
  }
}
