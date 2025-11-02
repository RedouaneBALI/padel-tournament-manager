package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TournamentBuilder {

  private TournamentBuilder() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Propagate winners across all phases sequentially.
   */
  public static void propagateWinners(Tournament tournament) {
    if (tournament == null) {
      return;
    }
    List<TournamentPhase> phases = buildPhases(tournament.getConfig());
    if (!phases.isEmpty()) {
      phases.getFirst().propagateWinners(tournament);
    }
  }

  public static List<String> validate(Tournament tournament) {
    List<String> errors = new ArrayList<>();
    if (tournament == null) {
      errors.add("Tournament is null");
      return errors;
    }
    TournamentConfig config = tournament.getConfig();
    try {
      validateConfig(config);
    } catch (IllegalArgumentException e) {
      errors.add(e.getMessage());
      return errors;
    }
    int mainDrawSize = config.getMainDrawSize();
    int nbSeeds      = config.getNbSeeds();
    if (mainDrawSize <= 0 || (mainDrawSize & (mainDrawSize - 1)) != 0) {
      errors.add("mainDrawSize must be a power of 2, got: " + mainDrawSize);
    }
    if (nbSeeds > mainDrawSize) {
      errors.add("nbSeeds (" + nbSeeds + ") cannot exceed mainDrawSize (" + mainDrawSize + ")");
    }
    List<TournamentPhase> phases = buildPhases(config);
    for (TournamentPhase phase : phases) {
      errors.addAll(phase.validate(tournament));
    }
    return errors;
  }

  /**
   * Complete tournament setup: creates structure and places players using the configured draw mode. This is the recommended public API method.
   *
   * @param tournament tournament with config set (will be modified)
   * @param initialRounds pre-populated initial rounds to replace the empty structure
   */
  public static void setupTournamentWithInitialRounds(Tournament tournament, List<Round> initialRounds) {
    if (tournament == null || tournament.getConfig() == null) {
      throw new IllegalArgumentException("Tournament and config cannot be null");
    }

    if (initialRounds == null || initialRounds.isEmpty()) {
      // Only initialize if tournament has no rounds yet
      if (tournament.getRounds().isEmpty()) {
        initializeEmptyRounds(tournament);
      }
      return;
    }

    // If tournament already has rounds, update them instead of recreating
    if (!tournament.getRounds().isEmpty()) {
      updateExistingRoundsWithNewGames(tournament, initialRounds);
      return;
    }

    // First time setup: create all rounds from scratch
    List<Round> allRounds = new ArrayList<>(createEmptyRounds(tournament.getConfig()));
    Map<Stage, Round> providedRoundsByStage = initialRounds.stream()
                                                           .collect(Collectors.toMap(Round::getStage, Function.identity()));
    for (int i = 0; i < allRounds.size(); i++) {
      Round round         = allRounds.get(i);
      Round providedRound = providedRoundsByStage.get(round.getStage());
      if (providedRound != null) {
        allRounds.set(i, providedRound);
      }
    }
    tournament.getRounds().clear();
    tournament.getRounds().addAll(allRounds);
  }

  /**
   * Updates existing rounds with new games from initial rounds. This preserves MatchFormat and other round configurations.
   *
   * @param tournament tournament with existing rounds
   * @param initialRounds rounds with new games to copy
   */
  private static void updateExistingRoundsWithNewGames(Tournament tournament, List<Round> initialRounds) {
    Map<Stage, Round> newRoundsByStage = initialRounds.stream()
                                                      .collect(Collectors.toMap(Round::getStage, Function.identity()));

    for (Round existingRound : tournament.getRounds()) {
      Round newRound = newRoundsByStage.get(existingRound.getStage());
      if (newRound != null) {
        // Copy games and pools from newRound to existingRound
        copyGamesAndPools(existingRound, newRound);
      } else {
        // Clear games in rounds that are not in initialRounds
        clearGamesInRound(existingRound);
      }
    }
  }

  /**
   * Copies games and pools from source round to target round, preserving MatchFormat.
   *
   * @param targetRound the round to update
   * @param sourceRound the round with new data
   */
  private static void copyGamesAndPools(Round targetRound, Round sourceRound) {
    // Clear existing games and pools
    targetRound.getGames().clear();
    targetRound.getPools().clear();

    // Copy games with proper format assignment
    for (Game sourceGame : sourceRound.getGames()) {
      Game newGame = new Game();
      newGame.setTeamA(sourceGame.getTeamA());
      newGame.setTeamB(sourceGame.getTeamB());
      newGame.setFormat(targetRound.getMatchFormat()); // Use the preserved MatchFormat
      targetRound.getGames().add(newGame);
    }

    // Copy pools if present
    if (sourceRound.getPools() != null && !sourceRound.getPools().isEmpty()) {
      targetRound.getPools().addAll(sourceRound.getPools());
    }
  }

  /**
   * Clears all games in a round (used for non-initial rounds during draw generation).
   *
   * @param round the round to clear
   */
  private static void clearGamesInRound(Round round) {
    round.getGames().forEach(game -> {
      game.setTeamA(null);
      game.setTeamB(null);
    });
  }

  /**
   * Sets up the tournament structure by replacing existing rounds with empty ones. No players are placed - use manual placement for MANUAL mode.
   *
   * @param tournament tournament with config set (will be modified)
   */
  public static void initializeEmptyRounds(Tournament tournament) {
    if (tournament == null || tournament.getConfig() == null) {
      throw new IllegalArgumentException("Tournament and config cannot be null");
    }
    List<Round> rounds = createEmptyRounds(tournament.getConfig());
    tournament.getRounds().clear();
    tournament.getRounds().addAll(rounds);
  }

  /**
   * Creates an empty tournament structure (rounds and games) based on the format configuration. Returns a list of rounds with empty games - no
   * players are placed. This is an internal method used by initializeEmptyRounds().
   *
   * @param config the tournament configuration containing format and other settings
   * @return list of rounds with empty games ready for manual or automatic population
   */
  private static List<Round> createEmptyRounds(TournamentConfig config) {
    validateConfig(config);
    return buildPhases(config).stream()
                              .flatMap(phase -> phase.initialize(config).stream())
                              .toList();
  }

  /**
   * Centralise la création des phases selon la config
   */
  private static List<TournamentPhase> buildPhases(TournamentConfig cfg) {
    List<TournamentPhase> phases = new ArrayList<>();
    TournamentFormat      format = cfg.getFormat();
    if (format == null) {
      return phases;
    }
    switch (format) {
      case KNOCKOUT:
        phases.add(new KnockoutPhase(cfg.getMainDrawSize(), cfg.getNbSeeds(), PhaseType.MAIN_DRAW));
        break;
      case QUALIF_KO:
        if (cfg.getPreQualDrawSize() != null && cfg.getPreQualDrawSize() > 0 && cfg.getNbQualifiers() != null && cfg.getNbQualifiers() > 0
            && cfg.getNbSeedsQualify() != null) {
          phases.add(new KnockoutPhase(cfg.getPreQualDrawSize(), cfg.getNbSeedsQualify(), PhaseType.QUALIFS));
        }
        phases.add(new KnockoutPhase(cfg.getMainDrawSize(), cfg.getNbSeeds(), PhaseType.MAIN_DRAW));
        break;
      case GROUPS_KO:
        if (cfg.getNbPools() != null && cfg.getNbPairsPerPool() > 0 && cfg.getNbQualifiedByPool() > 0) {
          phases.add(new GroupPhase(cfg.getNbPools(), cfg.getNbPairsPerPool(), cfg.getNbQualifiedByPool()));
        }
        phases.add(new KnockoutPhase(cfg.getMainDrawSize(), cfg.getNbSeeds(), PhaseType.MAIN_DRAW));
        break;
    }
    return phases;
  }

  /**
   * Validation centralisée de la config
   */
  private static void validateConfig(TournamentConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Tournament config cannot be null");
    }
    if (config.getFormat() == null) {
      throw new IllegalArgumentException("Tournament format cannot be null");
    }
  }

}
