package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KnockoutRoundGenerator extends AbstractRoundGenerator implements RoundGenerator {

  public KnockoutRoundGenerator(final List<PlayerPair> pairs, final int nbSeeds) {
    super(pairs, nbSeeds);
  }

  @Override
  public Round generate() {
    List<Game>       games     = createEmptyGames(getPairs().size());
    List<PlayerPair> remaining = placeSeedAndByeTeams(games, getPairs(), getNbSeeds());
    placeRemainingTeamsRandomly(games, remaining, getNbSeeds());
    Round round = new Round();
    round.setGames(games);
    round.setStage(Stage.fromNbTeams(getPairs().size()));
    return round;
  }

  private List<Game> createEmptyGames(int nbTeams) {
    int bracketSize = 1;
    while (bracketSize < nbTeams) {
      bracketSize *= 2;
    }

    int        nbGames = bracketSize / 2;
    List<Game> games   = new ArrayList<>();

    for (int i = 0; i < nbGames; i++) {
      games.add(new Game(new MatchFormat()));
    }

    return games;
  }

  /**
   * Place seeds and bye teams at the stratégic positions
   */
  private List<PlayerPair> placeSeedAndByeTeams(List<Game> games, List<PlayerPair> pairs, int nbSeeds) {
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

    // On retourne une nouvelle liste : uniquement les paires restantes à placer
    return new ArrayList<>(
        pairs.stream()
             .filter(p -> !seeds.contains(p) && !byeTeams.contains(p))
             .toList()
    );
  }

  /**
   * Place aléatoirement les équipes non-seeds dans les positions libres
   */
  private void placeRemainingTeamsRandomly(List<Game> games, List<PlayerPair> remainingTeams, int nbSeeds) {
    Collections.shuffle(remainingTeams);

    int teamIndex = 0;
    for (Game game : games) {
      if (teamIndex >= remainingTeams.size()) {
        break;
      }

      if (game.getTeamA() != null && game.getTeamB() == null) {
        // Compléter un match qui a déjà une seed
        game.setTeamB(remainingTeams.get(teamIndex++));
      } else if (game.getTeamA() == null) {
        // Remplir un match complètement vide
        game.setTeamA(remainingTeams.get(teamIndex++));
        if (teamIndex < remainingTeams.size()) {
          game.setTeamB(remainingTeams.get(teamIndex++));
        }
      }
    }
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
