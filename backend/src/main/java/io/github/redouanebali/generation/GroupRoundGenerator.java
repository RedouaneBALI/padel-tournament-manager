package io.github.redouanebali.generation;

import io.github.redouanebali.model.Group;
import io.github.redouanebali.model.PlayerPair;
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
      Group group = new Group();
      group.setName("Poule " + (char) ('A' + i));
      round.getGroups().add(group);
    }

    // Assign player pairs to each group in block order (not round-robin)
    List<Group> groups = round.getGroups().stream().toList();
    int         index  = 0;
    for (Group group : groups) {
      for (int j = 0; j < nbTeamPerPool; j++) {
        if (index < getPairs().size()) {
          group.getPairs().add(getPairs().get(index++));
        }
      }
    }

    generateGroupGames(round);

    return round;
  }

  private void generateGroupGames(Round round) {
    for (Group group : round.getGroups()) {
      List<PlayerPair> pairList = new ArrayList<>(group.getPairs());
      for (int i = 0; i < pairList.size(); i++) {
        for (int j = i + 1; j < pairList.size(); j++) {
          round.addGame(pairList.get(i), pairList.get(j));
        }
      }
    }
  }

}
