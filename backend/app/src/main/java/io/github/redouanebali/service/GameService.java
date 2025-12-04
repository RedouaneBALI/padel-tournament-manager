package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.UpdateGameRequest;
import io.github.redouanebali.dto.response.ScoreDTO;
import io.github.redouanebali.dto.response.UpdateScoreDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
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
  private final GamePointManager      gamePointManager;

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
    // --- Always use score history for any update (even direct set update) ---
    Score currentScore = game.getScore();
    if (currentScore == null) {
      currentScore = new Score();
      game.setScore(currentScore);
    }
    currentScore.saveToHistory();
    // Copy values from request.getScore() to currentScore (do not replace instance)
    Score newScore = request.getScore();
    if (newScore != null) {
      currentScore.getSets().clear();
      if (newScore.getSets() != null) {
        currentScore.getSets().addAll(newScore.getSets());
      }
      currentScore.setForfeit(newScore.isForfeit());
      currentScore.setForfeitedBy(newScore.getForfeitedBy());
      currentScore.setTieBreakPointA(newScore.getTieBreakPointA());
      currentScore.setTieBreakPointB(newScore.getTieBreakPointB());
      currentScore.setCurrentGamePointA(newScore.getCurrentGamePointA());
      currentScore.setCurrentGamePointB(newScore.getCurrentGamePointB());
      var sets = currentScore.getSets();
      if (!sets.isEmpty()) {
        SetScore lastSet = sets.get(sets.size() - 1);

        // Only add a new set if the last set is won AND the match is not finished
        if (isSetWin(lastSet.getTeamAScore(), lastSet.getTeamBScore()) && !game.isFinished()) {
          sets.add(new SetScore(0, 0));
        }

        // Synchronize tie-break points at root with last set if in tie-break
        if (lastSet.getTieBreakTeamA() != null || lastSet.getTieBreakTeamB() != null) {
          currentScore.setTieBreakPointA(lastSet.getTieBreakTeamA());
          currentScore.setTieBreakPointB(lastSet.getTieBreakTeamB());
        } else {
          currentScore.setTieBreakPointA(null);
          currentScore.setTieBreakPointB(null);
        }
      }
    }
    // --- End history logic ---
    return updateScoreAndPropagate(game, tournament, currentScore);
  }

  private UpdateScoreDTO updateScoreAndPropagate(Game game, Tournament tournament, Score score) {
    try {
      game.setScore(score);

      // Optimized: only propagate from the round containing this game onwards
      drawGenerationService.propagateWinnersFromGame(tournament, game);

      TeamSide winner = null;
      if (game.isFinished()) {
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
  public UpdateScoreDTO incrementGamePoint(Long tournamentId, Long gameId, TeamSide teamSide) {
    Game       game       = findGameInTournament(tournamentId, gameId);
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    gamePointManager.updateGamePoint(game, teamSide);
    return updateScoreAndPropagate(game, tournament, game.getScore());
  }

  @Transactional
  public UpdateScoreDTO undoGamePoint(Long tournamentId, Long gameId) {
    Game       game       = findGameInTournament(tournamentId, gameId);
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    gamePointManager.undoGamePoint(game);
    return updateScoreAndPropagate(game, tournament, game.getScore());
  }

  // Utilitaire local pour éviter la dépendance circulaire
  private boolean isSetWin(int gamesWinner, int gamesLoser) {
    return (gamesWinner == 6 && gamesWinner - gamesLoser >= 2) || gamesWinner == 7;
  }
}