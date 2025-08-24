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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupRoundGenerator extends AbstractRoundGenerator implements RoundGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(GroupRoundGenerator.class);
  private final        int    nbPools;
  private final        int    nbTeamPerPool;
  private final        int    nbQualifiedByPool;

  public GroupRoundGenerator(final int nbSeeds, int nbPools, int nbTeamPerPool, int nbQualifiedByPool) {
    super(nbSeeds);
    this.nbPools           = nbPools;
    this.nbTeamPerPool     = nbTeamPerPool;
    this.nbQualifiedByPool = nbQualifiedByPool;
  }

  private List<Round> generateRounds(final List<PlayerPair> pairs, final boolean manual) {
    List<Round> rounds = new ArrayList<>();

    // Build GROUPS round according to mode
    Round groups;
    if (manual) {
      groups = buildGroupsRoundManual(pairs);
    } else {
      groups = buildGroupsRoundAlgorithmic(pairs);
    }
    rounds.add(groups);

    // Build empty finals structure
    rounds.addAll(createFinalRoundsStructure());

    return rounds;
  }

  @Override
  public List<Round> generateAlgorithmicRounds(final List<PlayerPair> pairs) {
    return generateRounds(pairs, false);
  }

  @Override
  public List<Round> generateManualRounds(List<PlayerPair> pairs) {
    return generateRounds(pairs, true);
  }

  /*
  public List<Round> createRoundsStructure(final Tournament tournament) {
    List<Round> rounds = new ArrayList<>();

    // group phase
    Round round = new Round();
    round.setStage(Stage.GROUPS);

    MatchFormat sharedFormat   = round.getMatchFormat();
    int         nbPools        = this.nbPools;
    int         nbPairsPerPool = this.nbTeamPerPool;

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
    int   nbTeamsInFinaleBracket = this.nbPools * this.nbQualifiedByPool;
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
  } */

  // --- Helpers extracted to reduce duplication ---
  private Round createGroupsRoundSkeleton() {
    Round round = new Round();
    round.setStage(Stage.GROUPS);
    // Create named pools A, B, C, ...
    for (int i = 0; i < nbPools; i++) {
      Pool pool = new Pool();
      pool.setName("" + (char) ('A' + i));
      round.getPools().add(pool);
    }
    return round;
  }

  private Round buildGroupsRoundAlgorithmic(final List<PlayerPair> pairs) {
    Round round = createGroupsRoundSkeleton();

    if (this.getNbSeeds() > 0) {
      // Distribute seeds in zigzag pattern to balance pools
      List<Pool> pools     = new ArrayList<>(round.getPools());
      boolean    forward   = true;
      int        poolIndex = 0;

      for (int i = 0; i < Math.min(this.getNbSeeds(), pairs.size()); i++) {
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
      // Fallback to manual-like block assignment if there are no seeds
      buildGroupsRoundManualInternal(round, pairs);
    }

    generateGroupGames(round);
    return round;
  }

  private Round buildGroupsRoundManual(final List<PlayerPair> pairs) {
    Round round = createGroupsRoundSkeleton();
    buildGroupsRoundManualInternal(round, pairs);
    generateGroupGames(round);
    return round;
  }

  private void buildGroupsRoundManualInternal(Round round, final List<PlayerPair> pairs) {
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

  private List<Round> createFinalRoundsStructure() {
    List<Round> finals                 = new ArrayList<>();
    int         nbTeamsInFinaleBracket = this.nbPools * this.nbQualifiedByPool;
    Stage       current                = Stage.fromNbTeams(nbTeamsInFinaleBracket);
    while (current != null && current != Stage.WINNER) {
      Round r = new Round(current);

      MatchFormat matchFormat = r.getMatchFormat();
      if (matchFormat != null && matchFormat.getId() == null) {
        r.setMatchFormat(matchFormat);
      }

      int        nbMatches = current.getNbTeams() / 2;
      List<Game> games     = new ArrayList<>();
      for (int i = 0; i < nbMatches; i++) {
        Game game = new Game(matchFormat);
        games.add(game);
      }
      r.addGames(games);
      finals.add(r);

      current = current.next();
    }
    return finals;
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

  @Override
  public void propagateWinners(Tournament tournament) {
    if (tournament == null) {
      LOG.warn("propagateWinners: tournament is null");
      return;
    }

    this.updatePoolRankingIfNeeded(tournament.getRounds());

    int nbPools           = this.nbPools;
    int nbQualifiedByPool = this.nbQualifiedByPool;

    if (nbPools <= 0 || nbQualifiedByPool <= 0) {
      LOG.warn("propagateWinners: invalid pools ({}), qualifiedByPool ({})", nbPools, nbQualifiedByPool);
      return;
    }

    int totalQualified = nbPools * nbQualifiedByPool;

    // check power of two
    if ((totalQualified & (totalQualified - 1)) != 0) {
      LOG.error("Le nombre d'équipes qualifiées ({}) n'est pas une puissance de 2. Abandon de la propagation.", totalQualified);
      return;
    }

    if (nbQualifiedByPool > 2) {
      LOG.error("Propagation non gérée pour {} qualifiés par poule (>2).", nbQualifiedByPool);
      return;
    }

    // Find the GROUPS round
    Round groupsRound = tournament.getRounds().stream()
                                  .filter(r -> r.getStage() == Stage.GROUPS)
                                  .findFirst()
                                  .orElse(null);

    if (groupsRound == null) {
      LOG.warn("propagateWinners: aucun round GROUPS trouvé");
      return;
    }

    // Seed KO bracket only when all group matches are finished
    boolean groupsDone = groupsRound.getGames() != null && groupsRound.getGames().stream().allMatch(Game::isFinished);
    if (!groupsDone) {
      LOG.info("Group stage not finished yet; postponing KO seeding.");
      return;
    }

    // Determine next stage for bracket
    Stage nextStage = Stage.fromNbTeams(totalQualified);
    if (nextStage == null) {
      LOG.error("Aucun stage correspondant pour {} équipes.", totalQualified);
      return;
    }

    // Create next round (or reuse if already exists and matches nextStage)
    Round nextRound = tournament.getRounds().stream()
                                .filter(r -> r.getStage() == nextStage)
                                .findFirst()
                                .orElseGet(() -> {
                                  Round r = new Round(nextStage);
                                  // ensure a MatchFormat is present (reuse default in Round)
                                  tournament.getRounds().add(r);
                                  return r;
                                });

    // If next round already started (any game finished), don't reseed; only propagate winners.
    boolean nextRoundStarted = nextRound.getGames() != null && nextRound.getGames().stream().anyMatch(Game::isFinished);
    if (nextRoundStarted) {
      LOG.info("Next round {} already started, skipping reseed and only propagating winners.", nextStage);
      new KnockoutRoundGenerator(0).propagateWinners(tournament);
      return;
    }

    // Clear any placeholder games in the next round before initial seeding
    nextRound.getGames().clear();

    List<Pool> pools = new ArrayList<>(groupsRound.getPools());

    // Safety: ensure pools are in alphabetical order by their name (A,B,C,...) if names exist
    pools.sort((p1, p2) -> {
      String n1 = p1.getName() == null ? "" : p1.getName();
      String n2 = p2.getName() == null ? "" : p2.getName();
      return n1.compareTo(n2);
    });

    if (nbQualifiedByPool == 1) {
      // A1 vs B1, C1 vs D1, ...
      for (int i = 0; i + 1 < pools.size(); i += 2) {
        PlayerPair a1 = firstQualified(pools.get(i), 0);
        PlayerPair b1 = firstQualified(pools.get(i + 1), 0);
        if (a1 == null || b1 == null) {
          LOG.warn("Poule(s) incomplète(s) pour appairer {}1 vs {}1", pools.get(i).getName(), pools.get(i + 1).getName());
          continue;
        }
        nextRound.addGame(a1, b1);
      }
    } else { // nbQualifiedByPool == 2
      // A1 vs B2, B1 vs A2, C1 vs D2, D1 vs C2, ...
      for (int i = 0; i + 1 < pools.size(); i += 2) {
        Pool       pX = pools.get(i);
        Pool       pY = pools.get(i + 1);
        PlayerPair x1 = firstQualified(pX, 0);
        PlayerPair x2 = firstQualified(pX, 1);
        PlayerPair y1 = firstQualified(pY, 0);
        PlayerPair y2 = firstQualified(pY, 1);

        if (x1 == null || x2 == null || y1 == null || y2 == null) {
          LOG.warn("Paires insuffisantes pour appairer {} et {} (besoin des deux premiers).", pX.getName(), pY.getName());
          continue;
        }

        nextRound.addGame(x1, y2);
        nextRound.addGame(y1, x2);
      }
    }

    LOG.info("Propagation des qualifiés effectuée: {} matchs créés pour le stage {}.", nextRound.getGames().size(), nextStage);
    // Propagate winners through subsequent knockout rounds as well
    new KnockoutRoundGenerator(0).propagateWinners(tournament);
  }

  private PlayerPair firstQualified(Pool pool, int index) {
    if (pool == null) {
      return null;
    }

    // Prefer ranking if available
    if (pool.getPoolRanking() != null && pool.getPoolRanking().getDetails() != null) {
      List<io.github.redouanebali.model.PoolRankingDetails> details = pool.getPoolRanking().getDetails();
      if (index >= 0 && index < details.size()) {
        PlayerPair ranked = details.get(index).getPlayerPair();
        if (ranked != null) {
          return ranked;
        }
      }
    }

    // Fallback: insertion order in the set (if ranking unavailable)
    if (pool.getPairs() == null) {
      return null;
    }
    List<PlayerPair> pairs = new ArrayList<>(pool.getPairs());
    if (index < 0 || index >= pairs.size()) {
      return null;
    }
    return pairs.get(index);
  }

  private void updatePoolRankingIfNeeded(List<Round> sortedRounds) {
    Optional<Round> round = sortedRounds.stream().filter(r -> r.getStage() == Stage.GROUPS).findFirst();
    if (round.isEmpty()) {
      return;
    }

    for (Pool pool : round.get().getPools()) {
      List<Game> poolGames = round.get().getGames().stream()
                                  .filter(g -> pool.getPairs().contains(g.getTeamA()) || pool.getPairs().contains(g.getTeamB()))
                                  .toList();

      pool.recalculateRanking(poolGames);
    }
  }
}
