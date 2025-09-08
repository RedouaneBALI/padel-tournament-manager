package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GroupPhase implements TournamentPhase {

  private int nbPools;
  private int nbPairsPerPool;
  private int nbQualifiedByPool;

  @Override
  public List<String> validate(final Tournament tournament) {
    return List.of();
  }

  @Override
  public List<Round> initialize(final Tournament tournament) {
    Round round = new Round();
    round.setStage(Stage.GROUPS);
    return List.of(round);
  }

  @Override
  public void placeSeedTeams(final Round round, final List<PlayerPair> playerPairs) {
    // For groups, generally no seeds, so nothing to do
  }

  @Override
  public List<Integer> getSeedsPositions() {
    return List.of();
  }

  @Override
  public void placeByeTeams(final Round round, final int totalPairs) {
    // For groups, generally no BYEs, so nothing to do
  }

  @Override
  public void placeRemainingTeamsRandomly(final Round round, final List<PlayerPair> remainingTeams) {
    if (round == null || round.getGames() == null || remainingTeams == null || remainingTeams.isEmpty()) {
      return;
    }
    int index = 0;
    for (var game : round.getGames()) {
      if (game.getTeamA() == null && index < remainingTeams.size()) {
        game.setTeamA(remainingTeams.get(index++));
      }
      if (game.getTeamB() == null && index < remainingTeams.size()) {
        game.setTeamB(remainingTeams.get(index++));
      }
      if (index >= remainingTeams.size()) {
        break;
      }
    }
  }

  @Override
  public void propagateWinners(final Tournament tournament) {

  }

  @Override
  public Stage getInitialStage() {
    return Stage.GROUPS;
  }
}
