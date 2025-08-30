package io.github.redouanebali.generationV2;

import io.github.redouanebali.model.Tournament;

public final class TournamentBuilder {

  public Tournament buildQualifKO(Tournament t) {
    var cfg = t.getConfig();

    if (cfg.getPreQualDrawSize() > 0 && cfg.getNbQualifiers() > 0) {
      t = new KnockoutPhase(
          cfg.getPreQualDrawSize(),
          cfg.getNbSeedsQualify(),
          PhaseType.QUALIFS,
          cfg.getDrawMode()
      ).initialize(t);
    }

    t = new KnockoutPhase(
        cfg.getMainDrawSize(),
        cfg.getNbSeeds(),
        PhaseType.MAIN_DRAW,
        cfg.getDrawMode()
    ).initialize(t);

    return t;
  }
}
