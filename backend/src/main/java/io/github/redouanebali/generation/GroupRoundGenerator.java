package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import java.util.ArrayList;
import java.util.List;

public class GroupRoundGenerator extends AbstractRoundGenerator implements RoundGenerator {

  private final int nbPools;
  private final int nbTeamPerPool;

  public GroupRoundGenerator(final List<PlayerPair> pairs, final int nbSeeds, int nbPools, int nbTeamPerPool) {
    super(pairs, nbSeeds);
    this.nbPools       = nbPools;
    this.nbTeamPerPool = nbTeamPerPool;
  }

  @Override
  public Round generate() {
    Round round = new Round();
    round.setStage(Stage.GROUPS);

    // Create groups and assign names
    for (int i = 0; i < nbPools; i++) {
      Pool pool = new Pool();
      pool.setName("Poule " + (char) ('A' + i));
      round.getPools().add(pool);
    }

    // Assign player pairs to each group in block order (not round-robin)
    List<Pool> pools = round.getPools().stream().toList();
    int        index = 0;
    for (Pool pool : pools) {
      for (int j = 0; j < nbTeamPerPool; j++) {
        if (index < getPairs().size()) {
          pool.addPair(getPairs().get(index++));
        }
      }
    }

    generateGroupGames(round);

    return round;
  }

  private void generateGroupGames(Round round) {
    for (Pool pool : round.getPools()) {
      List<PlayerPair> pairList = new ArrayList<>(pool.getPairs());
      for (int i = 0; i < pairList.size(); i++) {
        for (int j = i + 1; j < pairList.size(); j++) {
          round.addGame(pairList.get(i), pairList.get(j));
        }
      }
    }
  }

}
