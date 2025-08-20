package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public class QualifRoudGenerator extends AbstractRoundGenerator {

  public QualifRoudGenerator(final int nbSeeds) {
    super(nbSeeds);
  }

  @Override
  public void propagateWinners(final Tournament tournament) {

  }

  @Override
  public Round generateAlgorithmicRound(final List<PlayerPair> pairs) {
    return null;
  }

  @Override
  public Round generateManualRound(final List<PlayerPair> pairs) {
    return null;
  }

  @Override
  public List<Round> initRoundsAndGames(final Tournament tournament) {
    return List.of();
  }
}
