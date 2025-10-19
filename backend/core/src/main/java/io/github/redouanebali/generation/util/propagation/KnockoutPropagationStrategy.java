package io.github.redouanebali.generation.util.propagation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Knockout strategy: propagates winners in a strict single-elimination bracket. Used when the number of matches in the next round is exactly half of
 * the current round (e.g. 16 → 8). Each match in the current round feeds a precise position in the next round, according to the binary structure of
 * the bracket.
 */
@RequiredArgsConstructor
public class KnockoutPropagationStrategy implements PropagationStrategy {

  private final WinnerPropagationUtil util;

  @Override
  public boolean placeWinner(List<Game> nextGames, int currentGameIndex, PlayerPair winner) {
    int idx = currentGameIndex / 2;
    if (idx < nextGames.size()) {
      Game ng = nextGames.get(idx);
      // Si winner est null, on réinitialise le slot
      if (currentGameIndex % 2 == 0) {
        if (winner == null) {
          ng.setTeamA(null);
          return true;
        }
      } else {
        if (winner == null) {
          ng.setTeamB(null);
          return true;
        }
      }
      return util.assignWinnerToSlot(ng, (currentGameIndex % 2 == 0), winner);
    }
    return false;
  }
}
