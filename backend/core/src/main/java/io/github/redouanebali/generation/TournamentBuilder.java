package io.github.redouanebali.generation;

import io.github.redouanebali.generation.draw.DrawStrategy;
import io.github.redouanebali.generation.draw.DrawStrategyFactory;
import io.github.redouanebali.generation.util.TournamentStageUtil;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TournamentBuilder {

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
      initializeEmptyRounds(tournament);
      return;
    }
    List<Round> allRounds = createEmptyRounds(tournament.getConfig());
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
   * Convenience method for automatic tournament setup. Builds initial rounds automatically using SEEDED strategy, then calls
   * setupTournamentWithInitialRounds().
   *
   * @param tournament tournament with config set (will be modified)
   * @param playerPairs players to automatically place in the tournament
   */
  public static void setupAndPopulateTournament(Tournament tournament, List<PlayerPair> playerPairs) {
    if (tournament == null || tournament.getConfig() == null) {
      throw new IllegalArgumentException("Tournament and config cannot be null");
    }
    if (playerPairs == null || playerPairs.isEmpty()) {
      setupTournamentWithInitialRounds(tournament, List.of());
      return;
    }
    List<Round> automaticRounds = buildAutomaticRounds(tournament, playerPairs);
    setupTournamentWithInitialRounds(tournament, automaticRounds);
  }

  /**
   * Builds initial rounds automatically using the SEEDED strategy. This method creates only the initial rounds that need to be populated (Q1, first
   * main round, etc.).
   *
   * @param tournament tournament configuration
   * @param playerPairs players to place
   * @return list of populated initial rounds
   */
  private static List<Round> buildAutomaticRounds(Tournament tournament, List<PlayerPair> playerPairs) {
    Tournament tempTournament = new Tournament();
    tempTournament.setConfig(tournament.getConfig());
    initializeEmptyRounds(tempTournament);
    DrawStrategy drawStrategy = DrawStrategyFactory.createStrategy(DrawMode.SEEDED);
    drawStrategy.placePlayers(tempTournament, playerPairs);
    return tempTournament.getRounds().stream()
                         .filter(round -> TournamentStageUtil.isInitialRoundInTournament(round, tempTournament.getRounds()))
                         .collect(Collectors.toList());
  }

  /**
   * Sets up the tournament structure by replacing existing rounds with empty ones. No players are placed - use setupAndPopulateTournament() for
   * complete setup.
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
                              .collect(Collectors.toList());
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
