package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public class GroupPhase implements TournamentPhase {

  @Override
  public List<String> validate(final Tournament tournament) {
    return List.of();
  }

  @Override
  public List<Round> initialize(final Tournament tournament) {
    return List.of();
  }

  @Override
  public void placeSeedTeams(final Round round, final List<PlayerPair> playerPairs, final int nbSeeds) {

  }

  @Override
  public List<Integer> getSeedsPositions(final int drawSize, final int nbSeeds) {
    return List.of();
  }

  @Override
  public void placeByeTeams(final Round round, final int totalPairs, final int drawSize, final int nbSeeds) {

  }

  @Override
  public void placeRemainingTeamsRandomly(final Round round, final List<PlayerPair> remainingTeams) {

  }

  @Override
  public void propagateWinners(final Tournament tournament) {

  }

  @Override
  public Round setRoundGames(final Round round, final List<Game> games) {
    return null;
  }
}
