package io.github.redouanebali.service;

import io.github.redouanebali.dto.GameUpdateRequest;
import io.github.redouanebali.dto.ScoreUpdateResponse;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.ScoreRepository;
import io.github.redouanebali.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {

  private final ScoreRepository              scoreRepository;
  private final GameRepository               gameRepository;
  private final TournamentRepository         tournamentRepository;
  private final TournamentProgressionService progressionService;
  private final TournamentService            tournamentService;

  public ScoreUpdateResponse updateGameScore(Long tournamentId, Long gameId, Score score) {
    Game game = gameRepository.findById(gameId)
                              .orElseThrow(() -> new IllegalArgumentException("Game not found with ID: " + gameId));
    Tournament tournament = tournamentService.getTournamentById(tournamentId);

    validateGameBelongsToTournament(gameId, tournament);

    return updateScoreAndPropagate(game, tournament, score);
  }

  public ScoreUpdateResponse updateGame(Long tournamentId, Long gameId, GameUpdateRequest request) {
    Game game = gameRepository.findById(gameId)
                              .orElseThrow(() -> new IllegalArgumentException("Game not found with ID: " + gameId));

    Tournament tournament = tournamentService.getTournamentById(tournamentId);

    validateGameBelongsToTournament(gameId, tournament);

    // Update time and court
    game.setScheduledTime(request.getScheduledTime());
    game.setCourt(request.getCourt());

    return updateScoreAndPropagate(game, tournament, request.getScore());
  }

  private ScoreUpdateResponse updateScoreAndPropagate(Game game, Tournament tournament, Score score) {
    Score persistedScore = scoreRepository.save(score);
    game.setScore(persistedScore);
    gameRepository.save(game);

    TeamSide winner = null;
    if (game.isFinished()) {
      progressionService.propagateWinners(tournament);
      tournamentRepository.save(tournament);
      winner = game.getWinner().equals(game.getTeamA()) ? TeamSide.TEAM_A : TeamSide.TEAM_B;
    }

    return new ScoreUpdateResponse(game.isFinished(), winner);
  }

  private void validateGameBelongsToTournament(Long gameId, Tournament tournament) {
    boolean belongsToTournament = tournament.getRounds().stream()
                                            .flatMap(round -> round.getGames().stream())
                                            .anyMatch(g -> g.getId().equals(gameId));

    if (!belongsToTournament) {
      throw new IllegalArgumentException("Game does not belong to the tournament");
    }
  }

}
