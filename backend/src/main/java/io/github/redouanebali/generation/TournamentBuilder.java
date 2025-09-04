package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class TournamentBuilder {

  private final List<TournamentPhase> phases = new ArrayList<>();

  public List<Round> buildQualifKOStructure(Tournament t) {
    var         cfg    = t.getConfig();
    List<Round> rounds = new ArrayList<>();
    phases.clear();

    if (cfg.getPreQualDrawSize() != null && cfg.getPreQualDrawSize() > 0 && cfg.getNbQualifiers() != null && cfg.getNbQualifiers() > 0) {
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

  public List<Round> buildGroupsKOStructure(Tournament t) {
    var         cfg    = t.getConfig();
    List<Round> rounds = new ArrayList<>();
    phases.clear();

    if (cfg.getNbPools() != null && cfg.getNbPairsPerPool() > 0 && cfg.getNbQualifiedByPool() > 0) {
      TournamentPhase groupPhase = new GroupPhase(
          cfg.getNbPools(),
          cfg.getNbPairsPerPool(),
          cfg.getNbQualifiedByPool()
      );
      rounds.addAll(groupPhase.initialize(t));
      phases.add(groupPhase);
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

  /**
   * Fills only the initial rounds of the tournament with the provided pairs, using each phase's logic (seeds, BYEs, etc.). This method only processes
   * the first round of each phase (e.g., Q1 and R32, or GROUPS and R16).
   *
   * @param tournament the tournament to fill
   * @param allPairs the complete list of pairs (sorted as needed)
   */
  public void drawLotsAndFillInitialRounds(Tournament tournament, List<PlayerPair> allPairs) {
    if (tournament == null || allPairs == null || allPairs.isEmpty() || phases.isEmpty()) {
      return;
    }
    List<Round> rounds = tournament.getRounds();
    if (rounds == null || rounds.isEmpty()) {
      return;
    }

    for (TournamentPhase phase : phases) {
      // Find the initial round using the phase's initial stage
      Stage initialStage = phase.getInitialStage();
      Round initialRound = rounds.stream()
                                 .filter(r -> r.getStage() == initialStage)
                                 .findFirst()
                                 .orElse(null);

      if (initialRound != null) {
        int roundDrawSize   = initialRound.getGames().size() * 2;
        int totalByesNeeded = roundDrawSize - allPairs.size();
        int configuredSeeds = tournament.getConfig().getNbSeeds();

        // Place the best teams (seeds + additional teams to match BYEs if needed)
        // This ensures all teams that get BYEs are the best ranked teams
        int teamsToProtectWithByes = Math.max(configuredSeeds, totalByesNeeded);

        // Place only the configured number of seeds in their proper positions
        phase.placeSeedTeams(initialRound, allPairs, teamsToProtectWithByes);

        // Place BYEs opposite to the protected teams (using the larger number for BYE protection)
        phase.placeByeTeams(initialRound, allPairs.size(), roundDrawSize, teamsToProtectWithByes);

        // Filter out teams that are already placed (seeds and those protected with BYEs)
        List<PlayerPair> remainingTeams = new ArrayList<>();
        for (int i = teamsToProtectWithByes; i < allPairs.size(); i++) {
          remainingTeams.add(allPairs.get(i));
        }

        // Place remaining teams in available slots
        phase.placeRemainingTeamsRandomly(initialRound, remainingTeams);
      }
    }
  }

  /**
   * Fills the initial rounds (Q1 and first round of main draw) in manual mode. The rounds and teams are already defined by the user. Only the Q1 and
   * first round of main draw are replaced, others remain unchanged.
   *
   * @param tournament the tournament to fill
   * @param initialRounds the list of rounds to use (Q1, then first round of main draw)
   */
  public void fillInitialRoundsManual(Tournament tournament, List<Round> initialRounds) {
    if (initialRounds == null || initialRounds.isEmpty()) {
      return;
    }
    List<Round> rounds = tournament.getRounds();
    if (rounds == null || rounds.isEmpty()) {
      return;
    }

    // Assume initialRounds[0] = Q1, initialRounds[1] = first round of main draw
    int replaced = 0;
    for (int i = 0; i < rounds.size() && replaced < initialRounds.size(); i++) {
      Round r        = rounds.get(i);
      Round provided = initialRounds.get(replaced);

      // Replace Q1 if stage matches
      if (replaced == 0 && r.getStage().name().equals(provided.getStage().name())) {
        rounds.set(i, provided);
        replaced++;
      }
      // Replace first round of main draw
      else if (replaced == 1 && r.getStage().name().equals(provided.getStage().name())) {
        rounds.set(i, provided);
        replaced++;
      }
    }
  }
}
