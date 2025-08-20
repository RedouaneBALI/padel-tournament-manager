package io.github.redouanebali.model.format;

import io.github.redouanebali.model.Tournament;
import java.util.List;

public class QualifMainStrategy implements FormatStrategy<QualifMainConfig> {

  @Override
  public void validate(QualifMainConfig cfg, List<String> errors) {
    if (!DrawMath.isPowerOfTwo(cfg.getPreQualDrawSize())) {
      errors.add("preQualDrawSize must be a power of two.");
    }
    if (!DrawMath.isPowerOfTwo(cfg.getMainDrawSize())) {
      errors.add("mainDrawSize must be a power of two.");
    }
    if (cfg.getNbSeeds() > cfg.getMainDrawSize()) {
      errors.add("nbSeeds > mainDrawSize.");
    }
    int filled = cfg.getDirectAcceptances() + cfg.getWildcards() + cfg.getNumQualifiers();
    if (filled > cfg.getMainDrawSize()) {
      errors.add("Direct+WC+Qualifiers exceed mainDrawSize.");
    }
  }

  @Override
  public List<StageKey> stages(QualifMainConfig cfg) {
    return List.of(StageKey.PRE_QUALIF, StageKey.MAIN_DRAW);
  }

  @Override
  public void buildInitialRounds(Tournament t, QualifMainConfig cfg) {
    // TODO: build PRE_QUALIF (KO) then MAIN_DRAW with Q1..Qk slots, seeds, BYEs.
  }
}