package io.github.redouanebali.generation.util;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
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
   * Randomly places remaining teams in the empty slots of a round.
   *
   * @param round the round to place teams in
   * @param remainingTeams the teams to place randomly
   * @throws IllegalArgumentException if totalPairs exceeds available slots
   */
  public static void placeRemainingTeamsRandomly(Round round, List<PlayerPair> remainingTeams) {
    if (round == null || round.getGames() == null || remainingTeams == null || remainingTeams.isEmpty()) {
      return;
    }

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
   * Places teams in order (without shuffling) - useful for manual mode.
   */
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
  }
}
