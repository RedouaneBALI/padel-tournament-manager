package io.github.redouanebali.generation;

import io.github.redouanebali.generation.util.ByePlacementUtil;
import io.github.redouanebali.generation.util.RandomPlacementUtil;
import io.github.redouanebali.generation.util.SeedPlacementUtil;
import io.github.redouanebali.generation.util.StaggeredSeedPlacementUtil;
import io.github.redouanebali.generation.util.propagation.WinnerPropagationUtil;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KnockoutPhase implements TournamentPhase {

  private final WinnerPropagationUtil winnerPropagationUtil = new WinnerPropagationUtil();
  private       int                   drawSize;
  private       int                   nbSeeds;
  private       PhaseType             phaseType;

  @Override
  public List<String> validate(final Tournament tournament) {
    return List.of();
  }

  @Override
  public List<Round> initialize(final TournamentConfig config) {
    validateConfig(config);
    return buildRounds(config);
  }

  private void validateConfig(TournamentConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("config is null");
    }
    if (drawSize <= 0 || (drawSize & (drawSize - 1)) != 0) {
      throw new IllegalArgumentException("drawSize must be a power of two");
    }
  }

  private List<Round> buildRounds(TournamentConfig config) {
    final List<Round> rounds = new ArrayList<>();

    switch (phaseType) {
      case QUALIFS -> buildQualificationRounds(rounds, config);
      case MAIN_DRAW -> buildMainDrawRounds(rounds);
      default -> throw new IllegalStateException("Unsupported phaseType: " + phaseType);
    }

    return rounds;
  }

  private void buildQualificationRounds(List<Round> rounds, TournamentConfig config) {
    validateQualificationConfig(config);

    int slots  = drawSize;
    int qIndex = 1;

    while (slots / 2 >= config.getNbQualifiers() && qIndex <= 3) {
      rounds.add(buildRound(Stage.fromQualifIndex(qIndex), slots / 2));
      slots /= 2;
      qIndex++;
    }
  }

  private void validateQualificationConfig(TournamentConfig config) {
    if (config.getNbQualifiers() <= 0
        || (config.getNbQualifiers() & (config.getNbQualifiers() - 1)) != 0
        || config.getNbQualifiers() > drawSize) {
      throw new IllegalArgumentException("nbQualifiers must be a power of two in (0, drawSize]");
    }
  }

  private void buildMainDrawRounds(List<Round> rounds) {
    int slots = drawSize;
    while (slots >= 2) {
      Stage stage = Stage.fromNbTeams(slots);
      rounds.add(buildRound(stage, slots / 2));
      slots /= 2;
    }
  }

  private Round buildRound(Stage stage, int nbGames) {
    Round r = new Round();
    r.setStage(stage);
    List<Game> games = new ArrayList<>(nbGames);
    for (int i = 0; i < nbGames; i++) {
      Game game = new Game();
      game.setFormat(r.getMatchFormat());
      games.add(game);
    }
    r.replaceGames(games);
    return r;
  }

  @Override
  public void placeSeedTeams(final Round round, final List<PlayerPair> playerPairs) {
    SeedPlacementUtil.placeSeedTeams(round, playerPairs, this.nbSeeds, this.drawSize);
  }

  /**
   * Place seeds teams for staggered entry tournaments.
   */
  public void placeSeedTeamsStaggered(final Round round,
                                      final List<PlayerPair> playerPairs,
                                      Stage currentStage,
                                      int mainDrawSize,
                                      int totalSeeds,
                                      boolean isFirstRound) {
    StaggeredSeedPlacementUtil.placeSeedTeamsStaggered(
        round, playerPairs, currentStage, mainDrawSize, totalSeeds, isFirstRound);
  }

  @Override
  public void placeByeTeams(final Round round, final int totalPairs) {
    ByePlacementUtil.placeByeTeams(round, totalPairs, this.nbSeeds, this.drawSize);
  }

  @Override
  public void placeRemainingTeamsRandomly(final Round round, final List<PlayerPair> remainingTeams) {
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, remainingTeams);
  }

  @Override
  public void propagateWinners(final Tournament tournament) {
    winnerPropagationUtil.propagateWinners(tournament);
  }

  @Override
  public Stage getInitialStage() {
    return switch (phaseType) {
      case QUALIFS -> Stage.Q1;
      case MAIN_DRAW -> Stage.fromNbTeams(drawSize);
      default -> throw new IllegalStateException("Unsupported phaseType: " + phaseType);
    };
  }
}
