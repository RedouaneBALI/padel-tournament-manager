package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.UpdateGameRequest;
import io.github.redouanebali.dto.response.UpdateScoreDTO;
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

  private final TournamentRepository  tournamentRepository;
  private final TournamentService     tournamentService;
  private final DrawGenerationService drawGenerationService;


  /**
   * Updates the score of a specific game and propagates winners if the game is finished. Saves the tournament after the update.
   *
   * @param tournamentId the tournament ID
   * @param gameId the game ID to update
   * @param score the new score to set
   * @return update result containing finish status and winner information
   * @throws IllegalArgumentException if tournament or game is not found
   */
  public UpdateScoreDTO updateGameScore(Long tournamentId, Long gameId, Score score) {
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    Game       game       = findGameInTournament(tournament, gameId);

    return updateScoreAndPropagate(game, tournament, score);
  }

  /**
   * Updates a game's complete information including score, scheduled time, and court. Propagates winners if the game becomes finished after the
   * update.
   *
   * @param tournamentId the tournament ID
   * @param gameId the game ID to update
   * @param request the update request containing score, time, and court information
   * @return update result containing finish status and winner information
   * @throws IllegalArgumentException if tournament or game is not found
   */
  public UpdateScoreDTO updateGame(Long tournamentId, Long gameId, UpdateGameRequest request) {
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    Game       game       = findGameInTournament(tournament, gameId);

    // Update time and court
    game.setScheduledTime(request.getScheduledTime());
    game.setCourt(request.getCourt());

    return updateScoreAndPropagate(game, tournament, request.getScore());
  }

  /**
   * Updates a game's score and propagates winners through the tournament if the game is finished. This is a common method used by both score-only and
   * full game updates.
   *
   * @param game the game to update
   * @param tournament the tournament containing the game
   * @param score the new score to set
   * @return update result with finish status and winner side information
   */
  private UpdateScoreDTO updateScoreAndPropagate(Game game, Tournament tournament, Score score) {
    game.setScore(score);

    TeamSide winner = null;
    if (game.isFinished()) {
      drawGenerationService.propagateWinners(tournament);
      winner = game.getWinner().equals(game.getTeamA()) ? TeamSide.TEAM_A : TeamSide.TEAM_B;
    }

    tournamentRepository.save(tournament);
    return new UpdateScoreDTO(game.isFinished(), winner);
  }


  /**
   * Finds a specific game within a tournament by its ID. Searches through all rounds and games in the tournament.
   *
   * @param tournament the tournament to search in
   * @param gameId the ID of the game to find
   * @return the found game
   * @throws IllegalArgumentException if no game with the given ID is found
   */
  private Game findGameInTournament(Tournament tournament, Long gameId) {
    return tournament.getRounds().stream()
                     .flatMap(round -> round.getGames().stream())
                     .filter(g -> g.getId().equals(gameId))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Game not found with ID: " + gameId));
  }

}
