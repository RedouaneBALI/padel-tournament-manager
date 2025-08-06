package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GroupRoundGenerator extends AbstractRoundGenerator implements RoundGenerator {

  private final int nbPools;
  private final int nbTeamPerPool;

  public GroupRoundGenerator(final int nbSeeds, int nbPools, int nbTeamPerPool) {
    super(nbSeeds);
    this.nbPools       = nbPools;
    this.nbTeamPerPool = nbTeamPerPool;
  }

  @Override
  public Round generateAlgorithmicRound(final List<PlayerPair> pairs) {
    // @todo
    return null;
  }

  @Override
  public Round generateManualRound(List<PlayerPair> pairs) {
    //     addMissingByePairsToReachPowerOfTwo(this.getPairs(), originalSize);
    Round round = new Round();
    round.setStage(Stage.GROUPS);

    // Create groups and assign names
    for (int i = 0; i < nbPools; i++) {
      Pool pool = new Pool();
      pool.setName("" + (char) ('A' + i));
      round.getPools().add(pool);
    }

    // Assign player pairs to each group in block order (not round-robin)
    List<Pool> pools = round.getPools().stream().toList();
    int        index = 0;
    for (Pool pool : pools) {
      for (int j = 0; j < nbTeamPerPool; j++) {
        if (index < pairs.size()) {
          pool.addPair(pairs.get(index++));
        }
      }
    }

    generateGroupGames(round);

    return round;
  }

  public List<Round> initRoundsAndGames(final Tournament tournament) {
    Round round = new Round();
    round.setStage(Stage.GROUPS);

    MatchFormat sharedFormat = round.getMatchFormat();

    int nbPools        = tournament.getNbPools();
    int nbPairsPerPool = tournament.getNbPairsPerPool();

    for (int i = 0; i < nbPools; i++) {
      Pool pool = new Pool();
      pool.setName("" + (char) ('A' + i));
      round.getPools().add(pool);

      List<Game> games     = new ArrayList<>();
      int        nbMatches = nbPairsPerPool * (nbPairsPerPool - 1) / 2;
      for (int j = 0; j < nbMatches; j++) {
        Game newGame = new Game(sharedFormat);
        newGame.setPool(pool);
        games.add(newGame); // Partager le même MatchFormat
      }
      round.addGames(games); // Assumant que vous avez cette méthode
    }

    return new ArrayList<>(Set.of(round));
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
