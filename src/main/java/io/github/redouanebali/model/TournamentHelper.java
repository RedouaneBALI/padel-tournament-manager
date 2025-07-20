package io.github.redouanebali.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TournamentHelper {

  public static List<Game> generateGames(List<PlayerPair> pairs, int nbSeeds) {
    // 1. Créer les matchs vides
    List<Game> games = createEmptyGames(pairs.size());

    // 2. Placer les têtes de série
    placeSeedTeams(games, pairs, nbSeeds);

    // 3. Placer les équipes restantes aléatoirement
    placeRemainingTeamsRandomly(games, pairs, nbSeeds);

    return games;
  }

  /**
   * Crée la structure de base des matchs (vides)
   */
  private static List<Game> createEmptyGames(int nbTeams) {
    List<Game> games = new ArrayList<>();
    for (int i = 0; i < nbTeams / 2; i++) {
      games.add(new Game());
    }
    return games;
  }

  /**
   * Place les têtes de série aux positions stratégiques
   */
  private static void placeSeedTeams(List<Game> games, List<PlayerPair> pairs, int nbSeeds) {
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));
    List<Integer> seedsPositions = getSeedsPositions(pairs.size(), nbSeeds);

    for (int i = 0; i < seedsPositions.size(); i++) {
      int gameIndex = seedsPositions.get(i) / 2;
      games.get(gameIndex).setTeamA(pairs.get(i));
    }
  }

  /**
   * Place aléatoirement les équipes non-seeds dans les positions libres
   */
  private static void placeRemainingTeamsRandomly(List<Game> games, List<PlayerPair> pairs, int nbSeeds) {
    List<PlayerPair> remainingTeams = new ArrayList<>(pairs.subList(nbSeeds, pairs.size()));
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
  public static List<Integer> getSeedsPositions(int nbTeams, int nbSeeds) {
    List<Integer> allPositions = generateAllSeedPositions(nbTeams);
    return allPositions.subList(0, Math.min(nbSeeds, allPositions.size()));
  }

  /**
   * Generate all the possible position recursively from the bracket structure
   */
  private static List<Integer> generateAllSeedPositions(int nbTeams) {
    if (nbTeams == 2) {
      return Arrays.asList(0, 1);
    }

    List<Integer> halfBracket = generateAllSeedPositions(nbTeams / 2);
    List<Integer> result      = new ArrayList<>();

    for (int i = 0; i < halfBracket.size(); i += 2) {
      int pos1 = halfBracket.get(i);
      int pos2 = halfBracket.get(i + 1);

      // Add positions for the first half
      result.add(pos1);
      result.add(nbTeams - 1 - pos1);

      // Add positions for the second half (reversed)
      result.add(nbTeams - 1 - pos2);
      result.add(pos2);
    }

    return result;
  }


}
