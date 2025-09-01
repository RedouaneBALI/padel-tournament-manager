package io.github.redouanebali.generationV2;

import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.List;

public final class TournamentBuilder {

  private final List<TournamentPhase> phases = new ArrayList<>();

  public List<Round> buildQualifKO(Tournament t) {
    var         cfg    = t.getConfig();
    List<Round> rounds = new ArrayList<>();
    phases.clear();

    if (cfg.getPreQualDrawSize() > 0 && cfg.getNbQualifiers() > 0) {
      TournamentPhase qualifs = new KnockoutPhase(
          cfg.getPreQualDrawSize(),
          cfg.getNbSeedsQualify(),
          PhaseType.QUALIFS,
          cfg.getDrawMode()
      );
      rounds.addAll(qualifs.initialize(t));
      phases.add(qualifs);
    }

    TournamentPhase mainDraw = new KnockoutPhase(
        cfg.getMainDrawSize(),
        cfg.getNbSeeds(),
        PhaseType.MAIN_DRAW,
        cfg.getDrawMode()
    );
    rounds.addAll(mainDraw.initialize(t));
    phases.add(mainDraw);

    return rounds;
  }

  /**
   * Propagate winners across all phases sequentially.
   */
  public void propagateWinners(Tournament t) {
    if (t == null || phases.isEmpty()) {
      return;
    }
    phases.getFirst().propagateWinners(t);
  }
}
