package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public abstract class AbstractRoundGenerator implements RoundGenerator {

  private final int nbSeeds;

  public void addMissingByePairsToReachPowerOfTwo(List<PlayerPair> pairs, int originalSize) {
    int powerOfTwo = 1;
    while (powerOfTwo < originalSize) {
      powerOfTwo *= 2;
    }
    int missing = powerOfTwo - originalSize;

    for (int i = 0; i < missing; i++) {
      PlayerPair bye = PlayerPair.bye();
      pairs.add(bye);
    }
  }


  /**
   * Optional propagation hook, overridden by specific generators if needed.
   */
  public abstract void propagateWinners(Tournament tournament);

  protected Round createEmptyRound(Stage stage, int nbGames) {
    Round r = new Round();
    r.setStage(stage);
    java.util.List<Game> list = new java.util.ArrayList<>(nbGames);
    for (int i = 0; i < nbGames; i++) {
      list.add(new Game(new MatchFormat()));
    }
    r.addGames(list);
    return r;
  }

  protected Round createManualRound(Stage stage,
                                    List<PlayerPair> pairs) {
    Round r = createEmptyRound(stage, pairs.size() / 2);
    for (int i = 0; i < pairs.size(); i++) {
      io.github.redouanebali.model.Game g = r.getGames().get(i / 2);
      if (i % 2 == 0) {
        g.setTeamA(pairs.get(i));
      } else {
        g.setTeamB(pairs.get(i));
      }
    }
    return r;
  }

  protected List<Game> createEmptyGames(int nbTeams) {
    int bracket = 1;
    while (bracket < nbTeams) {
      bracket <<= 1;
    }
    int                                               nbGames = bracket / 2;
    java.util.List<io.github.redouanebali.model.Game> list    = new java.util.ArrayList<>(nbGames);
    for (int i = 0; i < nbGames; i++) {
      list.add(new io.github.redouanebali.model.Game(new MatchFormat()));
    }
    return list;
  }

  protected int log2Safe(int n) {
    int r = 0;
    while (n > 1) {
      n >>= 1;
      r++;
    }
    return r;
  }
}
