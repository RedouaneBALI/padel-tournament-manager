package io.github.redouanebali.service;

import io.github.redouanebali.dto.GameUpdateRequest;
import io.github.redouanebali.dto.ScoreUpdateResponse;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {

  private final TournamentRepository         tournamentRepository;
  private final TournamentProgressionService progressionService;
  private final TournamentService            tournamentService;

  public ScoreUpdateResponse updateGameScore(Long tournamentId, Long gameId, Score score) {
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    Game       game       = findGameInTournament(tournament, gameId);

    return updateScoreAndPropagate(game, tournament, score);
  }

  public ScoreUpdateResponse updateGame(Long tournamentId, Long gameId, GameUpdateRequest request) {
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    Game       game       = findGameInTournament(tournament, gameId);

    // Update time and court
    game.setScheduledTime(request.getScheduledTime());
    game.setCourt(request.getCourt());

    return updateScoreAndPropagate(game, tournament, request.getScore());
  }

  private ScoreUpdateResponse updateScoreAndPropagate(Game game, Tournament tournament, Score score) {
    game.setScore(score);

    TeamSide winner = null;
    if (game.isFinished()) {
      progressionService.propagateWinners(tournament);
      winner = game.getWinner().equals(game.getTeamA()) ? TeamSide.TEAM_A : TeamSide.TEAM_B;
    }

    tournamentRepository.save(tournament);
    return new ScoreUpdateResponse(game.isFinished(), winner);
  }


  private Game findGameInTournament(Tournament tournament, Long gameId) {
    return tournament.getRounds().stream()
                     .flatMap(round -> round.getGames().stream())
                     .filter(g -> g.getId().equals(gameId))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Game not found with ID: " + gameId));
  }

}
