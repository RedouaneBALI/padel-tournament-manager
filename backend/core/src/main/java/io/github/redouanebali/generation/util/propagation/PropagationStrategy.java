package io.github.redouanebali.generation.util.propagation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import java.util.List;

/**
 * Strategy interface for propagating winners between rounds.
 */
public interface PropagationStrategy {

  /**
   * Place a winner in the next round's games.
   *
   * @param nextGames the list of games in the next round
   * @param currentGameIndex the index of the current game
   * @param winner the winner to propagate
   * @return true if the winner was placed, false otherwise
   */
  boolean placeWinner(List<Game> nextGames, int currentGameIndex, PlayerPair winner);
}

