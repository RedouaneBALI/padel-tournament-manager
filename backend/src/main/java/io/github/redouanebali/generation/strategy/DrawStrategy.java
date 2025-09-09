package io.github.redouanebali.generation.strategy;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public interface DrawStrategy {

  /**
   * Places players in the initial rounds of a tournament.
   *
   * @param tournament The tournament with its structure already initialized.
   * @param players The list of pairs to place.
   */
  void placePlayers(Tournament tournament, List<PlayerPair> players);
}
