package io.github.redouanebali.service;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.Tournament;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TournamentProgressionService {

  public PlayerPair getWinner(Game game) {
    if (!game.isFinished()) {
      return null;
    }

    int setsWonByA = 0;
    int setsWonByB = 0;

    for (SetScore set : game.getScore().getSets()) {
      if (set.getTeamAScore() > set.getTeamBScore()) {
        setsWonByA++;
      } else if (set.getTeamBScore() > set.getTeamAScore()) {
        setsWonByB++;
      }
    }
    return setsWonByA > setsWonByB ? game.getTeamA() : game.getTeamB();
  }


  public void propagateWinners(Tournament tournament) {
    List<Round> sortedRounds = tournament.getRounds().stream()
                                         .sorted(Comparator.comparing(Round::getStage))
                                         .toList();

    for (int roundIndex = 0; roundIndex < sortedRounds.size() - 1; roundIndex++) {
      Round currentRound = sortedRounds.get(roundIndex);
      Round nextRound    = sortedRounds.get(roundIndex + 1);

      List<Game> currentGames = currentRound.getGames();
      List<Game> nextGames    = nextRound.getGames();

      for (int i = 0; i < currentGames.size(); i++) {
        Game       currentGame = currentGames.get(i);
        PlayerPair winner      = null;

        if (currentGame.getTeamA() != null && currentGame.getTeamA().isBye()) {
          winner = currentGame.getTeamB();
        } else if (currentGame.getTeamB() != null && currentGame.getTeamB().isBye()) {
          winner = currentGame.getTeamA();
        } else if (currentGame.isFinished()) {
          winner = this.getWinner(currentGame);
        }

        int  targetGameIndex = i / 2;
        Game nextGame        = nextGames.get(targetGameIndex);

        if (winner == null) {
          // Match pas terminé => on ne met rien dans le match suivant
          if (i % 2 == 0) {
            nextGame.setTeamA(null);
          } else {
            nextGame.setTeamB(null);
          }
        } else {
          // On place le vainqueur dans la bonne position
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
