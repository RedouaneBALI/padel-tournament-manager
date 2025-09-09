package io.github.redouanebali.generation;

import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class TournamentBuilder {

  private final List<TournamentPhase> phases = new ArrayList<>();

  public List<Round> buildQualifKOStructure(TournamentFormatConfig cfg) {
    List<Round> rounds = new ArrayList<>();
    phases.clear();

    if (cfg.getPreQualDrawSize() != null && cfg.getPreQualDrawSize() > 0 && cfg.getNbQualifiers() != null && cfg.getNbQualifiers() > 0) {
      TournamentPhase qualifs = new KnockoutPhase(
          cfg.getPreQualDrawSize(),
          cfg.getNbSeedsQualify(),
          PhaseType.QUALIFS
      );
      rounds.addAll(qualifs.initialize(cfg));
      phases.add(qualifs);
    }

    TournamentPhase mainDraw = new KnockoutPhase(
        cfg.getMainDrawSize(),
        cfg.getNbSeeds(),
        PhaseType.MAIN_DRAW
    );
    rounds.addAll(mainDraw.initialize(cfg));
    phases.add(mainDraw);

    return rounds;
  }

  public List<Round> buildGroupsKOStructure(TournamentFormatConfig cfg) {
    List<Round> rounds = new ArrayList<>();
    phases.clear();

    if (cfg.getNbPools() != null && cfg.getNbPairsPerPool() > 0 && cfg.getNbQualifiedByPool() > 0) {
      TournamentPhase groupPhase = new GroupPhase(
          cfg.getNbPools(),
          cfg.getNbPairsPerPool(),
          cfg.getNbQualifiedByPool()
      );
      rounds.addAll(groupPhase.initialize(cfg));
      phases.add(groupPhase);
    }

    TournamentPhase mainDraw = new KnockoutPhase(
        cfg.getMainDrawSize(),
        cfg.getNbSeeds(),
        PhaseType.MAIN_DRAW
    );
    rounds.addAll(mainDraw.initialize(cfg));
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

  public List<String> validate(Tournament t) {
    List<String> errors = new ArrayList<>();
    if (t == null) {
      errors.add("Tournament is null");
      return errors;
    }

    var config = t.getConfig();
    if (config == null) {
      errors.add("Tournament configuration is null");
      return errors;
    }

    int mainDrawSize = config.getMainDrawSize();
    int nbSeeds      = config.getNbSeeds();

    // Validate mainDrawSize is a power of 2
    if (mainDrawSize <= 0 || (mainDrawSize & (mainDrawSize - 1)) != 0) {
      errors.add("mainDrawSize must be a power of 2, got: " + mainDrawSize);
    }

    // Validate nbSeeds doesn't exceed mainDrawSize
    if (nbSeeds > mainDrawSize) {
      errors.add("nbSeeds (" + nbSeeds + ") cannot exceed mainDrawSize (" + mainDrawSize + ")");
    }

    // Validate other phases if they exist
    if (!phases.isEmpty()) {
      for (TournamentPhase phase : phases) {
        errors.addAll(phase.validate(t));
      }
    }

    return errors;
  }
}
