package io.github.redouanebali.model.format.knockout;

import io.github.redouanebali.generation.KnockoutRoundGenerator;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMath;
import io.github.redouanebali.model.format.FormatStrategy;
import io.github.redouanebali.model.format.StageKey;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import java.util.List;

public class KnockoutStrategy implements FormatStrategy {

  @Override
  public void validate(TournamentFormatConfig cfg, List<String> errors) {
    if (!DrawMath.isPowerOfTwo(cfg.getMainDrawSize())) {
      errors.add("mainDrawSize must be a power of two.");
    }
    if (cfg.getNbSeeds() > cfg.getMainDrawSize()) {
      errors.add("nbSeeds > mainDrawSize.");
    }
  }

  @Override
  public List<StageKey> stages(TournamentFormatConfig cfg) {
    return List.of(StageKey.MAIN_DRAW);
  }

  @Override
  public void buildInitialRounds(Tournament t, TournamentFormatConfig cfg) {
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(cfg.getNbSeeds());
    List<Round>            rounds    = generator.createRoundsStructure(t);
    t.getRounds().clear();
    t.getRounds().addAll(rounds);
  }

  @Override
  public Round generateRound(Tournament t, List<PlayerPair> pairs, boolean manual) {
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(t.getConfig().getNbSeeds());
    return manual ? generator.generateManualRound(pairs)
                  : generator.generateAlgorithmicRound(pairs);
  }

  @Override
  public void propagateWinners(Tournament t) {
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(t.getConfig().getNbSeeds());
    generator.propagateWinners(t);
  }
}