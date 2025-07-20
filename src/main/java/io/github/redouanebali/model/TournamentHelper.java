package io.github.redouanebali.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class TournamentHelper {

  public static List<Game> initGamesWithSeedTeams(List<PlayerPair> pairs, int nbSeeds) {
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));
    List<Integer> seedsPositions = getSeedsPositions(pairs.size(), nbSeeds);
    List<Game>    games          = new ArrayList<>();
    for (int i = 0; i < pairs.size() / 2; i++) {
      Game game = new Game();
      games.add(game);
    }
    for (int i = 0; i < seedsPositions.size(); i++) {
      games.get(seedsPositions.get(i) / 2).setTeamA(pairs.get(i));
    }

    return games;
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
