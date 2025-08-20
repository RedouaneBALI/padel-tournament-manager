package io.github.redouanebali.model.format;

import io.github.redouanebali.generation.KnockoutRoundGenerator;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public class KnockoutStrategy implements FormatStrategy<KnockoutConfig> {

  @Override
  public void validate(KnockoutConfig cfg, List<String> errors) {
    if (!DrawMath.isPowerOfTwo(cfg.getMainDrawSize())) {
      errors.add("mainDrawSize must be a power of two.");
    }
    if (cfg.getNbSeeds() > cfg.getMainDrawSize()) {
      errors.add("nbSeeds > mainDrawSize.");
    }
  }

  @Override
  public List<StageKey> stages(KnockoutConfig cfg) {
    return List.of(StageKey.MAIN_DRAW);
  }

  @Override
  public void buildInitialRounds(Tournament t, KnockoutConfig cfg) {
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(cfg.getNbSeeds());
    List<Round>            rounds    = generator.initRoundsAndGames(t);
    t.getRounds().clear();
    t.getRounds().addAll(rounds);
  }

  @Override
  public Round generateRound(Tournament t, List<PlayerPair> pairs, boolean manual) {
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(t.getNbSeeds());
    return manual ? generator.generateManualRound(pairs)
                  : generator.generateAlgorithmicRound(pairs);
  }

  @Override
  public void propagateWinners(Tournament t) {
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(t.getNbSeeds());
    generator.propagateWinners(t);
  }
}