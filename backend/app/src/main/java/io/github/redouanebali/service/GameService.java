package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.UpdateGameRequest;
import io.github.redouanebali.dto.response.ScoreDTO;
import io.github.redouanebali.dto.response.UpdateScoreDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameService {

  private final TournamentRepository  tournamentRepository;
  private final TournamentService     tournamentService;
  private final DrawGenerationService drawGenerationService;
  private final TournamentMapper      tournamentMapper;
  private final GamePointManager      gamePointManager = new GamePointManager();

  // --- EXISTING METHODS (UNCHANGED) ---

  @Transactional
  public UpdateScoreDTO updateGameScore(Long tournamentId, Long gameId, Score score) {
    Game       game       = findGameInTournament(tournamentId, gameId);
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    return updateScoreAndPropagate(game, tournament, score);
  }

  @Transactional
  public UpdateScoreDTO updateGame(Long tournamentId, Long gameId, UpdateGameRequest request) {
    Game       game       = findGameInTournament(tournamentId, gameId);
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    game.setScheduledTime(request.getScheduledTime());
    game.setCourt(request.getCourt());
    return updateScoreAndPropagate(game, tournament, request.getScore());
  }

  private UpdateScoreDTO updateScoreAndPropagate(Game game, Tournament tournament, Score score) {
    try {
      game.setScore(score);

      TeamSide winner = null;
      if (game.isFinished()) {
        drawGenerationService.propagateWinners(tournament);
        winner = game.getWinner().equals(game.getTeamA()) ? TeamSide.TEAM_A : TeamSide.TEAM_B;
      }

      tournamentRepository.save(tournament);
      ScoreDTO scoreDTO = tournamentMapper.toDTO(game.getScore());
      return new UpdateScoreDTO(game.isFinished(), winner, scoreDTO);
    } catch (Exception e) {
      throw new RuntimeException("Failed to update game score for tournament " + tournament.getId() +
                                 ", game " + game.getId() + ": " + e.getMessage(), e);
    }
  }

  public Game findGameInTournament(Long tournamentId, Long gameId) {
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    return tournament.getRounds().stream()
                     .flatMap(round -> round.getGames().stream())
                     .filter(g -> g.getId().equals(gameId))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Game not found with ID: " + gameId));
  }

  @Transactional
  public UpdateScoreDTO updateGamePoint(Long tournamentId, Long gameId, TeamSide teamSide) {
    Game       game       = findGameInTournament(tournamentId, gameId);
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    gamePointManager.updateGamePoint(game, teamSide);
    // Force winnerSide update
    game.setScore(game.getScore());
    tournamentRepository.save(tournament);
    ScoreDTO scoreDTO = tournamentMapper.toDTO(game.getScore());
    return new UpdateScoreDTO(true, game.getWinnerSide(), scoreDTO);
  }

  @Transactional
  public UpdateScoreDTO undoGamePoint(Long tournamentId, Long gameId) {
    Game       game       = findGameInTournament(tournamentId, gameId);
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    gamePointManager.undoGamePoint(game);
    // Force winnerSide update
    game.setScore(game.getScore());
    tournamentRepository.save(tournament);
    ScoreDTO scoreDTO = tournamentMapper.toDTO(game.getScore());
    return new UpdateScoreDTO(true, game.getWinnerSide(), scoreDTO);
  }
}