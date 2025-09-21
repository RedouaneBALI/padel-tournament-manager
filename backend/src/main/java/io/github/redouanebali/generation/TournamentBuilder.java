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
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class TournamentBuilder {

  private final List<TournamentPhase> phases = new ArrayList<>();

  /**
   * Propagate winners across all phases sequentially.
   */
  public void propagateWinners(Tournament tournament) {
    if (tournament == null || phases.isEmpty()) {
      return;
    }
    phases.getFirst().propagateWinners(tournament);
  }

  public List<String> validate(Tournament tournament) {
    List<String> errors = new ArrayList<>();
    if (tournament == null) {
      errors.add("Tournament is null");
      return errors;
    }

    var config = tournament.getConfig();
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
        errors.addAll(phase.validate(tournament));
      }
    }

    return errors;
  }


  /**
   * Complete tournament setup: creates structure and places players using the configured draw mode. This is the recommended public API method.
   *
   * @param tournament tournament with config set (will be modified)
   * @param initialRounds pre-populated initial rounds to replace the empty structure
   */
  public void setupTournamentWithInitialRounds(Tournament tournament, List<Round> initialRounds) {
    if (tournament == null || tournament.getConfig() == null) {
      throw new IllegalArgumentException("Tournament and config cannot be null");
    }

    if (initialRounds == null || initialRounds.isEmpty()) {
      // Fallback to empty structure if no initial rounds provided
      initializeEmptyRounds(tournament);
      return;
    }

    // Create full structure first
    List<Round> allRounds = createEmptyRounds(tournament.getConfig());

    // Replace initial rounds with provided ones
    Map<Stage, Round> providedRoundsByStage = initialRounds.stream()
                                                           .collect(Collectors.toMap(Round::getStage, Function.identity()));

    for (int i = 0; i < allRounds.size(); i++) {
      Round round         = allRounds.get(i);
      Round providedRound = providedRoundsByStage.get(round.getStage());
      if (providedRound != null) {
        // Replace with provided round
        allRounds.set(i, providedRound);
      }
      // Keep empty rounds for subsequent stages
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
  public void setupAndPopulateTournament(Tournament tournament, List<PlayerPair> playerPairs) {
    if (tournament == null || tournament.getConfig() == null) {
      throw new IllegalArgumentException("Tournament and config cannot be null");
    }

    if (playerPairs == null || playerPairs.isEmpty()) {
      // Empty tournament case
      setupTournamentWithInitialRounds(tournament, List.of());
      return;
    }

    // Build automatic rounds using SEEDED strategy
    List<Round> automaticRounds = buildAutomaticRounds(tournament, playerPairs);

    // Use the unified method
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
  private List<Round> buildAutomaticRounds(Tournament tournament, List<PlayerPair> playerPairs) {
    // Create temporary tournament to use DrawStrategy
    Tournament tempTournament = new Tournament();
    tempTournament.setConfig(tournament.getConfig());
    initializeEmptyRounds(tempTournament);

    // Use SEEDED strategy to populate initial rounds
    DrawStrategy drawStrategy = DrawStrategyFactory.createStrategy(DrawMode.SEEDED);
    drawStrategy.placePlayers(tempTournament, playerPairs);

    // Extract only the initial rounds that were populated using utility
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
  public void initializeEmptyRounds(Tournament tournament) {
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
  private List<Round> createEmptyRounds(TournamentConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Tournament config cannot be null");
    }

    TournamentFormat format = config.getFormat();
    if (format == null) {
      throw new IllegalArgumentException("Tournament format cannot be null");
    }

    return switch (format) {
      case KNOCKOUT -> buildQualifKORounds(config);
      case QUALIF_KO -> buildQualifKORounds(config);
      case GROUPS_KO -> buildGroupsKORounds(config);
    };
  }

  private List<Round> buildQualifKORounds(TournamentConfig cfg) {
    List<Round> rounds = new ArrayList<>();
    //  phases.clear();

    if (cfg.getPreQualDrawSize() != null && cfg.getPreQualDrawSize() > 0 && cfg.getNbQualifiers() != null && cfg.getNbQualifiers() > 0
        && cfg.getNbSeedsQualify() != null && cfg.getNbSeedsQualify() > 0) {
      TournamentPhase qualifs = new KnockoutPhase(
          cfg.getPreQualDrawSize(),
          cfg.getNbSeedsQualify(),
          PhaseType.QUALIFS
      );
      rounds.addAll(qualifs.initialize(cfg));
      //    phases.add(qualifs);
    }

    TournamentPhase mainDraw = new KnockoutPhase(
        cfg.getMainDrawSize(),
        cfg.getNbSeeds(),
        PhaseType.MAIN_DRAW
    );
    rounds.addAll(mainDraw.initialize(cfg));
    //   phases.add(mainDraw);

    return rounds;
  }

  private List<Round> buildGroupsKORounds(TournamentConfig cfg) {
    List<Round> rounds = new ArrayList<>();
    //  phases.clear();

    if (cfg.getNbPools() != null && cfg.getNbPairsPerPool() > 0 && cfg.getNbQualifiedByPool() > 0) {
      TournamentPhase groupPhase = new GroupPhase(
          cfg.getNbPools(),
          cfg.getNbPairsPerPool(),
          cfg.getNbQualifiedByPool()
      );
      rounds.addAll(groupPhase.initialize(cfg));
      //    phases.add(groupPhase);
    }

    TournamentPhase mainDraw = new KnockoutPhase(
        cfg.getMainDrawSize(),
        cfg.getNbSeeds(),
        PhaseType.MAIN_DRAW
    );
    rounds.addAll(mainDraw.initialize(cfg));
//    phases.add(mainDraw);

    return rounds;
  }

}


