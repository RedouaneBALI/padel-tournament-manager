package io.github.redouanebali.generation;

import io.github.redouanebali.generation.util.RandomPlacementUtil;
import io.github.redouanebali.generation.util.SeedPlacementUtil;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMath;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GroupPhase implements TournamentPhase {

  private int nbPools;
  private int nbPairsPerPool;
  private int nbQualifiedByPool;

  @Override
  public List<String> validate(final Tournament tournament) {
    List<String> errors = new ArrayList<>();

    if (tournament == null) {
      errors.add("Tournament cannot be null");
      return errors;
    }

    TournamentFormatConfig config = tournament.getConfig();
    if (config == null) {
      errors.add("Tournament configuration cannot be null");
      return errors;
    }

    // Get total number of teams
    int totalTeams = tournament.getPlayerPairs() != null ? tournament.getPlayerPairs().size() : 0;

    // Check that total teams is a multiple of number of pools
    if (totalTeams > 0 && totalTeams % nbPools != 0) {
      errors.add("Total number of teams (" + totalTeams + ") must be a multiple of number of pools (" + nbPools + ")");
    }

    // Check that qualified per pool doesn't exceed pairs per pool
    if (nbQualifiedByPool > nbPairsPerPool) {
      errors.add("Number of qualified per pool (" + nbQualifiedByPool + ") cannot exceed pairs per pool (" + nbPairsPerPool + ")");
    }

    // Check that total qualified is a power of 2 (for knockout phase)
    int totalQualified = nbPools * nbQualifiedByPool;
    if (totalQualified > 0 && !DrawMath.isPowerOfTwo(totalQualified)) {
      errors.add("Total qualified teams (" + totalQualified + ") must be a power of 2 for knockout phase");
    }

    // Check basic constraints
    if (nbPools <= 0) {
      errors.add("Number of pools must be positive");
    }

    if (nbPairsPerPool <= 0) {
      errors.add("Number of pairs per pool must be positive");
    }

    if (nbQualifiedByPool <= 0) {
      errors.add("Number of qualified per pool must be positive");
    }

    return errors;
  }

  @Override
  public List<Round> initialize(final TournamentFormatConfig config) {
    Round round = new Round();
    round.setStage(Stage.GROUPS);

    // Create the pools
    for (int i = 0; i < nbPools; i++) {
      Pool pool = new Pool();
      pool.setName("Pool " + (char) ('A' + i)); // Pool A, Pool B, Pool C, etc.
      round.addPool(pool);
    }

    return List.of(round);
  }

  @Override
  public void placeSeedTeams(final Round round, final List<PlayerPair> playerPairs) {
    SeedPlacementUtil.placeSeedTeamsInPools(round, playerPairs, nbPairsPerPool);
    // Generate games after placing seeds (in case all teams are seeded)
    generateRoundRobinGamesIfPoolsAreFull(round);
  }

  @Override
  public List<Integer> getSeedsPositions() {
    return List.of();
  }

  @Override
  public void placeByeTeams(final Round round, final int totalPairs) {
    // For groups, generally no BYES, so nothing to do
  }

  @Override
  public void placeRemainingTeamsRandomly(final Round round, final List<PlayerPair> remainingTeams) {
    RandomPlacementUtil.placeRemainingTeamsRandomly(round, remainingTeams);

    // Generate round-robin games for each pool after all teams are placed
    generateRoundRobinGamesIfPoolsAreFull(round);
  }

  /**
   * Generate round-robin games only if all pools are full (have the expected number of teams). This prevents generating games multiple times.
   */
  private void generateRoundRobinGamesIfPoolsAreFull(Round round) {
    if (round == null || round.getPools().isEmpty()) {
      return;
    }

    // Check if all pools have the expected number of teams
    boolean allPoolsFull = round.getPools().stream()
                                .allMatch(pool -> pool.getPairs().size() == nbPairsPerPool);

    if (allPoolsFull) {
      generateRoundRobinGames(round);
    }
  }

  /**
   * Generate all possible round-robin games for each pool in the round. Each team in a pool plays against every other team exactly once.
   */
  private void generateRoundRobinGames(Round round) {
    if (round == null || round.getPools().isEmpty()) {
      return;
    }

    // Clear any existing games
    round.getGames().clear();

    for (Pool pool : round.getPools()) {
      List<PlayerPair> teams = pool.getPairs();

      // Generate all combinations of teams in this pool (round-robin)
      for (int i = 0; i < teams.size(); i++) {
        for (int j = i + 1; j < teams.size(); j++) {
          PlayerPair teamA = teams.get(i);
          PlayerPair teamB = teams.get(j);

          Game game = new Game();
          game.setTeamA(teamA);
          game.setTeamB(teamB);
          game.setFormat(round.getMatchFormat());

          round.addGame(game);
        }
      }
    }
  }

  @Override
  public void propagateWinners(final Tournament tournament) {

  }

  @Override
  public Stage getInitialStage() {
    return Stage.GROUPS;
  }
}
