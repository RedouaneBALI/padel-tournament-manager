package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
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

  public List<Round> generateRounds(Tournament tournament, List<PlayerPair> pairs, boolean manual) {
    if (manual) {
      return generateManualRounds(pairs);
    }
    return generateAlgorithmicRounds(pairs);
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
      list.add(new Game(r.getMatchFormat()));
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

  protected List<Game> createEmptyGames(int nbTeams, MatchFormat format) {
    int bracket = 1;
    while (bracket < nbTeams) {
      bracket <<= 1;
    }
    int        nbGames = bracket / 2;
    List<Game> list    = new ArrayList<>(nbGames);
    for (int i = 0; i < nbGames; i++) {
      list.add(new Game(format));
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

  /**
   * Place aléatoirement les équipes non-seeds dans les positions libres
   */
  public void placeRemainingTeamsRandomly(List<Game> games, List<PlayerPair> remainingTeams) {
    Collections.shuffle(remainingTeams);

    int teamIndex = 0;
    for (Game game : games) {
      if (teamIndex >= remainingTeams.size()) {
        break;
      }

      if (game.getTeamA() != null && game.getTeamB() == null) {
        game.setTeamB(remainingTeams.get(teamIndex++));
      } else if (game.getTeamA() == null) {
        game.setTeamA(remainingTeams.get(teamIndex++));
        if (teamIndex < remainingTeams.size()) {
          game.setTeamB(remainingTeams.get(teamIndex++));
        }
      }
    }
  }

  /**
   * Place seeds and bye teams at the stratégic positions
   */
  public List<PlayerPair> placeSeedAndByeTeams(List<Game> games, List<PlayerPair> pairs, int nbSeeds) {
    List<PlayerPair> seeds = pairs.stream()
                                  .filter(p -> !p.isBye())
                                  .sorted(Comparator.comparingInt(PlayerPair::getSeed))
                                  .limit(nbSeeds)
                                  .toList();

    List<PlayerPair> byeTeams = pairs.stream()
                                     .filter(PlayerPair::isBye)
                                     .toList();

    List<Integer> seedsPositions = getSeedsPositions(pairs.size(), nbSeeds);

    for (int i = 0; i < seeds.size(); i++) {
      int gameIndex = seedsPositions.get(i) / 2;
      games.get(gameIndex).setTeamA(seeds.get(i));

      if (i < byeTeams.size()) {
        games.get(gameIndex).setTeamB(byeTeams.get(i));
      }
    }

    return new ArrayList<>(
        pairs.stream()
             .filter(p -> !seeds.contains(p) && !byeTeams.contains(p))
             .toList()
    );
  }

  /**
   * Compute recursively the positions of the seeds in a tournament
   *
   * @param nbTeams total number of teams
   * @param nbSeeds number of seeds teams
   * @return list of the positions of the seeds
   */
  public List<Integer> getSeedsPositions(int nbTeams, int nbSeeds) {
    List<Integer> allPositions = generateAllSeedPositions(nbTeams);
    return allPositions.subList(0, Math.min(nbSeeds, allPositions.size()));
  }

  /**
   * Generate all the possible position recursively from the bracket structure
   */
  private List<Integer> generateAllSeedPositions(int nbTeams) {
    if (nbTeams <= 1) {
      return Collections.singletonList(0);
    }

    int powerOfTwo = 1;
    while (powerOfTwo < nbTeams) {
      powerOfTwo *= 2;
    }

    List<Integer> fullPositions = generatePerfectSeedPositions(powerOfTwo);

    return fullPositions.subList(0, nbTeams);
  }

  /**
   * Génère les positions des seeds pour un bracket parfait (nbTeams doit être une puissance de 2)
   */
  private List<Integer> generatePerfectSeedPositions(int nbTeams) {
    if (nbTeams == 1) {
      return Collections.singletonList(0);
    }

    List<Integer> prev   = generatePerfectSeedPositions(nbTeams / 2);
    List<Integer> result = new ArrayList<>();

    for (int i = 0; i < prev.size(); i++) {
      int pos = prev.get(i);
      if (i % 2 == 0) {
        // Pour les indices pairs, on met la position en première moitié
        result.add(pos);
        result.add(nbTeams - 1 - pos);
      } else {
        // Pour les indices impairs, on inverse l'ordre
        result.add(nbTeams - 1 - pos);
        result.add(pos);
      }
    }

    return result;
  }
  
}
