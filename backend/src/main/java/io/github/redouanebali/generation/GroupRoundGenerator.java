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
    Round round = new Round();
    round.setStage(Stage.GROUPS);

    // Create groups and assign names
    for (int i = 0; i < nbPools; i++) {
      Pool pool = new Pool();
      pool.setName("" + (char) ('A' + i));
      round.getPools().add(pool);
    }

    // Distribute seeds algorithmically
    if (this.getNbSeeds() > 0) {
      // Distribute seeds in zigzag pattern to balance pools
      List<Pool> pools     = new ArrayList<>(round.getPools());
      boolean    forward   = true;
      int        poolIndex = 0;

      for (int i = 0; i < this.getNbSeeds(); i++) {
        PlayerPair pair = pairs.get(i);
        pools.get(poolIndex).addPair(pair);

        if (forward) {
          poolIndex++;
          if (poolIndex >= pools.size()) {
            poolIndex = pools.size() - 1;
            forward   = false;
          }
        } else {
          poolIndex--;
          if (poolIndex < 0) {
            poolIndex = 0;
            forward   = true;
          }
        }
      }
      // Assign remaining pairs to pools in block order
      int index = this.getNbSeeds();
      outer:
      for (Pool pool : pools) {
        while (pool.getPairs().size() < nbTeamPerPool) {
          if (index >= pairs.size()) {
            break outer;
          }
          pool.addPair(pairs.get(index++));
        }
      }
    } else {
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
    }

    generateGroupGames(round);

    return round;
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
    List<Round> rounds = new ArrayList<>();

    // group phase
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
        games.add(newGame);
      }
      round.addGames(games);
    }
    rounds.add(round);

    // finale phase
    int   nbTeamsInFinaleBracket = tournament.getNbPools() * tournament.getNbQualifiedByPool();
    Stage current                = Stage.fromNbTeams(nbTeamsInFinaleBracket);
    while (current != null && current != Stage.WINNER) {
      round = new Round(current);

      MatchFormat matchFormat = round.getMatchFormat();
      if (matchFormat != null && matchFormat.getId() == null) {
        round.setMatchFormat(matchFormat);
      }

      int nbMatches = current.getNbTeams() / 2;

      List<Game> games = new ArrayList<>();
      for (int i = 0; i < nbMatches; i++) {
        Game game = new Game(matchFormat);
        games.add(game);
      }

      round.addGames(games);
      rounds.add(round);

      current = current.next();
    }
    return new ArrayList<>(rounds);
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
