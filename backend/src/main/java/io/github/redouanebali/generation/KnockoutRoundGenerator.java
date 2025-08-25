package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class KnockoutRoundGenerator extends AbstractRoundGenerator {

  public KnockoutRoundGenerator(final int nbSeeds) {
    super(nbSeeds);
  }

  public List<Round> generateRounds(List<PlayerPair> pairs, boolean manual) {
    int originalSize = pairs.size();
    addMissingByePairsToReachPowerOfTwo(pairs, originalSize);
    Stage       start  = Stage.fromNbTeams(pairs.size());
    List<Round> rounds = buildBracketFrom(start, pairs.size());
    Round       first  = rounds.getFirst();
    if (manual) {
      populateFirstRoundManual(first, pairs);
    } else {
      populateFirstRoundAlgorithmic(first, pairs);
    }
    return rounds;
  }

  @Override
  public List<Round> generateAlgorithmicRounds(List<PlayerPair> pairs) {
    return generateRounds(pairs, false);
  }

  @Override
  public List<Round> generateManualRounds(List<PlayerPair> pairs) {
    return generateRounds(pairs, true);
  }

  /**
   * Build full bracket rounds from the given starting stage down to (but excluding) WINNER.
   *
   * @param start The starting Stage.
   * @param startNbTeams The number of teams in the starting round.
   */
  private List<Round> buildBracketFrom(Stage start, int startNbTeams) {
    LinkedList<Round> rounds         = new LinkedList<>();
    Stage             cur            = start;
    int               teamsThisStage = startNbTeams; // e.g., 8 teams -> 4 matches for QUARTERS
    while (cur != null && cur != Stage.WINNER) {
      Round       r  = new Round(cur);
      MatchFormat mf = r.getMatchFormat();
      if (mf != null && mf.getId() == null) {
        r.setMatchFormat(mf);
      }
      int        nbMatches  = Math.max(teamsThisStage / 2, 0);
      List<Game> emptyGames = new ArrayList<>(nbMatches);
      for (int i = 0; i < nbMatches; i++) {
        emptyGames.add(new Game(r.getMatchFormat()));
      }
      r.addGames(emptyGames);
      rounds.add(r);
      teamsThisStage = Math.max(teamsThisStage / 2, 0); // halve for next stage
      cur            = cur.next();
    }
    return rounds;
  }

  /**
   * Populate the first round with seeds/BYEs and then remaining teams (algorithmic).
   */
  private void populateFirstRoundAlgorithmic(Round first, List<PlayerPair> pairs) {
    // reset any previous assignments
    for (Game g : first.getGames()) {
      g.setTeamA(null);
      g.setTeamB(null);
      g.setScore(null);
    }
    List<Game>       games     = first.getGames();
    List<PlayerPair> remaining = placeSeedAndByeTeams(games, pairs, getNbSeeds());
    placeRemainingTeamsRandomly(games, remaining);
  }

  /**
   * Populate the first round manually by assigning A then B sequentially.
   */
  private void populateFirstRoundManual(Round first, List<PlayerPair> pairs) {
    for (Game g : first.getGames()) {
      g.setTeamA(null);
      g.setTeamB(null);
      g.setScore(null);
    }
    int teamIndex = 0;
    for (Game game : first.getGames()) {
      if (teamIndex < pairs.size()) {
        game.setTeamA(pairs.get(teamIndex++));
      }
      if (teamIndex < pairs.size()) {
        game.setTeamB(pairs.get(teamIndex++));
      }
    }
  }

  public void propagateWinners(Tournament tournament) {
    if (tournament == null || tournament.getRounds() == null || tournament.getRounds().size() < 2) {
      return;
    }

    List<Round> rounds = tournament.getRounds();

    // Skip GROUPS; start from the first bracket round
    int startIndex = 0;
    while (startIndex < rounds.size() && rounds.get(startIndex).getStage() == Stage.GROUPS) {
      startIndex++;
    }

    for (int roundIndex = startIndex; roundIndex < rounds.size() - 1; roundIndex++) {
      Round currentRound = rounds.get(roundIndex);
      Round nextRound    = rounds.get(roundIndex + 1);

      // Skip propagation if currentRound is entirely empty (all games have null teamA and null teamB)
      boolean currentRoundEmpty = currentRound.getGames()
                                              .stream()
                                              .allMatch(g -> g.getTeamA() == null && g.getTeamB() == null);
      if (currentRoundEmpty) {
        continue;
      }

      // Safety: skip any non-bracket rounds just in case
      if (currentRound.getStage() == Stage.GROUPS || nextRound.getStage() == Stage.GROUPS) {
        continue;
      }

      List<Game> currentGames = currentRound.getGames();
      List<Game> nextGames    = nextRound.getGames();
      if (nextGames == null) {
        continue;
      }

      for (int i = 0; i < currentGames.size(); i++) {
        Game       currentGame = currentGames.get(i);
        PlayerPair winner      = null;

        if (currentGame.getTeamA() != null && currentGame.getTeamA().isBye()) {
          winner = currentGame.getTeamB();
        } else if (currentGame.getTeamB() != null && currentGame.getTeamB().isBye()) {
          winner = currentGame.getTeamA();
        } else if (currentGame.isFinished()) {
          winner = currentGame.getWinner();
        }

        int targetGameIndex = i / 2;
        if (targetGameIndex >= nextGames.size()) {
          // next round not large enough yet (shouldn't happen if rounds were initialized), skip safely
          continue;
        }
        Game nextGame = nextGames.get(targetGameIndex);

        if (winner == null) {
          if (i % 2 == 0) {
            nextGame.setTeamA(null);
          } else {
            nextGame.setTeamB(null);
          }
        } else {
          if (i % 2 == 0) {
            nextGame.setTeamA(winner);
          } else {
            nextGame.setTeamB(winner);
          }
        }
      }
    }
  }

}
