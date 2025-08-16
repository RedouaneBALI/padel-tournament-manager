package io.github.redouanebali.util;


import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
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
      gamesToWinPerSet = game.getFormat().getPointsPerSet();
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
    format.setPointsPerSet(6);
    format.setSuperTieBreakInFinalSet(false);
    return format;
  }
}
