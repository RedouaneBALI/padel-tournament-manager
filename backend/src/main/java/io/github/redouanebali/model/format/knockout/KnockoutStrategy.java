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
  public List<Round> initializeRounds(Tournament t, List<PlayerPair> pairs, boolean manual) {
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(t.getConfig().getNbSeeds());
    //  List<Round>            rounds    = generator.createRoundsStructure(t);
    //  t.getRounds().clear();
    //  t.getRounds().addAll(rounds);
    if (manual) {
      return generator.generateManualRounds(pairs);
    } else {
      return generator.generateAlgorithmicRounds(pairs);
    }
  }

  @Override
  public void propagateWinners(Tournament t) {
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(t.getConfig().getNbSeeds());
    generator.propagateWinners(t);
  }
}