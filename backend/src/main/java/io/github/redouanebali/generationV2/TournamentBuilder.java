package io.github.redouanebali.generationV2;

import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.List;

public final class TournamentBuilder {

  public List<Round> buildQualifKO(Tournament t) {
    var         cfg    = t.getConfig();
    List<Round> rounds = new ArrayList<>();

    if (cfg.getPreQualDrawSize() > 0 && cfg.getNbQualifiers() > 0) {
      List<Round> qualifRounds = new KnockoutPhase(
          cfg.getPreQualDrawSize(),
          cfg.getNbSeedsQualify(),
          PhaseType.QUALIFS,
          cfg.getDrawMode()
      ).initialize(t);
      rounds.addAll(qualifRounds);
    }

    List<Round> mainDrawRounds = new KnockoutPhase(
        cfg.getMainDrawSize(),
        cfg.getNbSeeds(),
        PhaseType.MAIN_DRAW,
        cfg.getDrawMode()
    ).initialize(t);
    rounds.addAll(mainDrawRounds);
    return rounds;
  }
}
