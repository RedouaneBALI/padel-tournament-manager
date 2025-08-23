package io.github.redouanebali.model.format.qualifymain;

import io.github.redouanebali.generation.QualifyMainRoundGenerator;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMath;
import io.github.redouanebali.model.format.FormatStrategy;
import io.github.redouanebali.model.format.StageKey;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import java.util.List;

public class QualifMainStrategy implements FormatStrategy {

  @Override
  public void validate(TournamentFormatConfig cfg, List<String> errors) {
    if (!DrawMath.isPowerOfTwo(cfg.getPreQualDrawSize())) {
      errors.add("preQualDrawSize must be a power of two.");
    }
    if (!DrawMath.isPowerOfTwo(cfg.getMainDrawSize())) {
      errors.add("mainDrawSize must be a power of two.");
    }
    if (cfg.getNbSeeds() > cfg.getMainDrawSize()) {
      errors.add("nbSeeds > mainDrawSize.");
    }
  }

  @Override
  public List<StageKey> stages(TournamentFormatConfig cfg) {
    return List.of(StageKey.PRE_QUALIF, StageKey.MAIN_DRAW);
  }

  @Override
  public void buildInitialRounds(Tournament t, TournamentFormatConfig cfg) {
    QualifyMainRoundGenerator generator = new QualifyMainRoundGenerator(
        cfg.getNbSeeds() != null ? cfg.getNbSeeds() : 0,
        cfg.getPreQualDrawSize() != null ? cfg.getPreQualDrawSize() : 0,
        cfg.getMainDrawSize() != null ? cfg.getMainDrawSize() : 0
    );
    List<Round> rounds = generator.createRoundsStructure(t);
    t.getRounds().clear();
    t.getRounds().addAll(rounds);
  }

  @Override
  public Round generateRound(Tournament t, List<PlayerPair> pairs, boolean manual) {
    QualifyMainRoundGenerator generator = new QualifyMainRoundGenerator(
        t.getConfig().getNbSeeds() != null ? t.getConfig().getNbSeeds() : 0,
        t.getConfig().getMainDrawSize() != null ? t.getConfig().getMainDrawSize() : 0,
        t.getConfig().getNbQualifiers() != null ? t.getConfig().getNbQualifiers() : 0
    );
    return manual ? generator.generateManualRound(pairs)
                  : generator.generateAlgorithmicRound(pairs);
  }

}