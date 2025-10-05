package io.github.redouanebali.generation.util.propagation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Generic strategy: propagates winners in non-classic cases. Used when the bracket structure is not strictly binary, for example for qualification
 * rounds, lucky losers, or any round where the number of matches in the next round does not match half of the current round.
 */
@RequiredArgsConstructor
public class QualifierSlotPropagationStrategy implements PropagationStrategy {

  private final WinnerPropagationUtil util;

  @Override
  public boolean placeWinner(List<Game> nextGames, int currentGameIndex, PlayerPair winner) {
    return util.placeWinnerInQualifierOrAvailableSlot(nextGames, winner);
  }
}
