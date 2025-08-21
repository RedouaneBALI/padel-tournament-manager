package io.github.redouanebali.model.format.qualifymain;

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
    // TODO: build PRE_QUALIF (KO) then MAIN_DRAW with Q1..Qk slots, seeds, BYEs.
  }

}