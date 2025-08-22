package io.github.redouanebali.util;


import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Shared test data builders to avoid duplication across tests.
 */
public final class TestFixtures {

  private TestFixtures() {
  }

  public static PlayerPair buildPairWithSeed(int seed) {
    Player player1 = new Player((long) seed, "Player" + seed + "A", seed, 0, 1990);
    Player player2 = new Player((long) seed + 100, "Player" + seed + "B", seed, 0, 1990);
    return new PlayerPair(-1L, player1, player2, seed);
  }

  /**
   * Create a deterministic list of PlayerPair with seeds 1..count.
   */
  public static List<PlayerPair> createPairs(int count) {
    List<PlayerPair> pairs = new ArrayList<>();
    IntStream.rangeClosed(1, count).forEach(seed -> pairs.add(buildPairWithSeed(seed)));
    return pairs;
  }

  /**
   * Create a single PlayerPair where the seed encodes pool & rank for deterministic ordering.
   */
  public static PlayerPair makePairFromPoolRank(int poolIndex, int rankInPool) {
    int seed = (poolIndex + 1) * 100 + rankInPool;
    return buildPairWithSeed(seed);
  }

  /**
   * Create a score where the given winner wins straight sets according to the game's MatchFormat. If format is null, defaults to 1 set to 6–0.
   */
  public static Score createScoreWithWinner(Game game, PlayerPair winner) {
    int setsToWin        = 1;
    int gamesToWinPerSet = 6;
    if (game.getFormat() != null) {
      setsToWin        = game.getFormat().getNumberOfSetsToWin();
      gamesToWinPerSet = game.getFormat().getGamesPerSet();
    }
    List<SetScore> sets = new ArrayList<>();
    for (int i = 0; i < setsToWin; i++) {
      SetScore set = new SetScore();
      if (game.getTeamA() == winner) {
        set.setTeamAScore(gamesToWinPerSet);
        set.setTeamBScore(0);
      } else {
        set.setTeamAScore(0);
        set.setTeamBScore(gamesToWinPerSet);
      }
      sets.add(set);
    }
    Score score = new Score();
    score.setSets(sets);
    return score;
  }

  public static boolean gameContainsBoth(Game g, PlayerPair p, PlayerPair q) {
    return (g.getTeamA() == p && g.getTeamB() == q) || (g.getTeamA() == q && g.getTeamB() == p);
  }

  public static MatchFormat createSimpleFormat(int nbSetToWin) {
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(nbSetToWin);
    format.setGamesPerSet(6);
    format.setSuperTieBreakInFinalSet(false);
    return format;
  }

  public static int roundRobinGamesPerPool(int pairsPerPool) {
    return pairsPerPool * (pairsPerPool - 1) / 2;
  }

  public static int totalGroupGames(int nbPools, int pairsPerPool) {
    return nbPools * roundRobinGamesPerPool(pairsPerPool);
  }

  public static List<Pool> sortedPoolsByName(List<Pool> pools) {
    List<Pool> list = new ArrayList<>(pools);
    list.sort(java.util.Comparator.comparing(p -> p.getName() == null ? "" : p.getName()));
    return list;
  }

  public static List<PlayerPair> sortedPairsBySeed(List<PlayerPair> pairs) {
    List<PlayerPair> list = new ArrayList<>(pairs);
    list.sort(java.util.Comparator.comparingInt(PlayerPair::getSeed));
    return list;
  }

  public static Round findRound(Tournament t, Stage stage) {
    return t.getRounds().stream().filter(r -> r.getStage() == stage).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));
  }

  public static void applyGeneratedGroups(Tournament t, Round generatedGroups) {
    Round groups = findRound(t, Stage.GROUPS);
    groups.getPools().clear();
    groups.getPools().addAll(generatedGroups.getPools());
    groups.getGames().clear();
    for (Game gg : generatedGroups.getGames()) {
      groups.addGame(gg.getTeamA(), gg.getTeamB());
    }
  }

  public static void simulatePoolWinners(Round groups,
                                         Pool pool,
                                         PlayerPair top1,
                                         PlayerPair top2) {
    for (Game g : groups.getGames()) {
      if (!(pool.getPairs().contains(g.getTeamA()) || pool.getPairs().contains(g.getTeamB()))) {
        continue;
      }
      g.setFormat(createSimpleFormat(1));
      PlayerPair ta      = g.getTeamA();
      PlayerPair tb      = g.getTeamB();
      PlayerPair winner;
      boolean    aInTop2 = ta == top1 || ta == top2;
      boolean    bInTop2 = tb == top1 || tb == top2;
      if (aInTop2 && bInTop2) {
        winner = top1; // break tie deterministically
      } else if (aInTop2) {
        winner = ta;
      } else if (bInTop2) {
        winner = tb;
      } else {
        winner = ta; // arbitrary
      }
      g.setScore(createScoreWithWinner(g, winner));
    }
  }

  public static java.util.Set<PlayerPair> teamsInRound(Round r) {
    java.util.Set<PlayerPair> teams = new java.util.HashSet<>();
    for (Game g : r.getGames()) {
      if (g.getTeamA() != null) {
        teams.add(g.getTeamA());
      }
      if (g.getTeamB() != null) {
        teams.add(g.getTeamB());
      }
    }
    return teams;
  }
}
