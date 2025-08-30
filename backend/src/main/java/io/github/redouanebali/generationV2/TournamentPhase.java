package io.github.redouanebali.generationV2;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public interface TournamentPhase {

  Tournament initialize(Tournament tournament);

  Round placeSeedTeams(List<Game> games, List<PlayerPair> playerPairs, int nbSeeds);

  /**
   * Compute recursively the positions of the seeds in a tournament
   *
   * @param drawSize size of the bracket
   * @param nbSeeds number of seeds teams
   * @return list of the positions of the seeds
   */
  List<Integer> getSeedsPositions(int drawSize, int nbSeeds);

  Round placeByeTeams(List<Game> games, int totalPairs, int drawSize, int nbSeeds);

  Round placeRemainingTeamsRandomly(List<Game> g, List<PlayerPair> remainingTeams);

  Tournament propagateWinners(Tournament t);

  Round setRoundGames(Round r, List<Game> g);

}
