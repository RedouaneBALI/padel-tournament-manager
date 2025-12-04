package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.dto.request.CreateStandaloneGameRequest;
import io.github.redouanebali.dto.response.ScoreDTO;
import io.github.redouanebali.dto.response.UpdateScoreDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.MatchFormatRepository;
import io.github.redouanebali.repository.PlayerPairRepository;
import io.github.redouanebali.security.SecurityUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for managing standalone games (matches without tournaments). Provides CRUD operations for simple matches between two player pairs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StandaloneGameService {

  private final GameRepository        gameRepository;
  private final PlayerPairRepository  playerPairRepository;
  private final MatchFormatRepository matchFormatRepository;
  private final TournamentMapper      tournamentMapper;
  private final GamePointManager      gamePointManager;

  /**
   * Creates a new standalone game with two player pairs and a match format.
   *
   * @param request the request containing team A, team B, and match format
   * @return the created game
   */
  @Transactional
  public Game createStandaloneGame(CreateStandaloneGameRequest request) {
    log.info("Creating standalone game between {} vs {}",
             request.getTeamA().getPlayer1Name() + "/" + request.getTeamA().getPlayer2Name(),
             request.getTeamB().getPlayer1Name() + "/" + request.getTeamB().getPlayer2Name());

    // Create and save player pairs
    PlayerPair teamA = createPlayerPair(request.getTeamA());
    PlayerPair teamB = createPlayerPair(request.getTeamB());

    // Persist the match format
    MatchFormat format = matchFormatRepository.save(request.getFormat());

    // Create the game
    Game game = new Game(format);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    game.setPool(null); // Standalone game, not part of any pool/tournament

    // Set owner
    String me = SecurityUtil.currentUserId();
    game.setCreatedBy(me);

    return gameRepository.save(game);
  }

  /**
   * Retrieves all standalone games (games not associated with any tournament).
   *
   * @return list of all standalone games
   */
  public List<Game> getAllStandaloneGames() {
    return gameRepository.findByPoolIsNull();
  }

  /**
   * Retrieves standalone games created by the given user.
   *
   * @param userId owner id
   * @return list of games
   */
  public List<Game> getStandaloneGamesByOwner(String userId) {
    return gameRepository.findByCreatedByAndPoolIsNull(userId);
  }

  /**
   * Retrieves a specific game by its ID.
   */
  public Game getGameById(Long id) {
    return gameRepository.findById(id)
                         .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found with ID: " + id));
  }

  @Transactional
  public UpdateScoreDTO incrementGamePoint(Long gameId, TeamSide teamSide) {
    Game game = getGameById(gameId);
    gamePointManager.incrementGamePoint(game, teamSide);
    game.setScore(game.getScore()); // refresh winner side when a game finishes
    gameRepository.save(game);
    ScoreDTO scoreDTO = tournamentMapper.toDTO(game, game.getScore());
    return new UpdateScoreDTO(game.isFinished(), game.getWinnerSide(), scoreDTO);
  }

  @Transactional
  public UpdateScoreDTO undoGamePoint(Long gameId) {
    Game game = getGameById(gameId);
    gamePointManager.undoGamePoint(game);
    game.setScore(game.getScore());
    gameRepository.save(game);
    ScoreDTO scoreDTO = tournamentMapper.toDTO(game, game.getScore());
    return new UpdateScoreDTO(game.isFinished(), game.getWinnerSide(), scoreDTO);
  }

  /**
   * Deletes a standalone game.
   *
   * @param id the game ID to delete
   */
  @Transactional
  public void deleteGame(Long id) {
    Game game = getGameById(id);
    log.info("Deleting standalone game with ID: {}", id);
    gameRepository.delete(game);
  }

  /**
   * Creates a PlayerPair from a CreatePlayerPairRequest.
   *
   * @param request the request containing player names
   * @return the created and saved player pair
   */
  private PlayerPair createPlayerPair(CreatePlayerPairRequest request) {
    Player player1 = new Player(request.getPlayer1Name());
    Player player2 = new Player(request.getPlayer2Name());

    PlayerPair pair = new PlayerPair();
    pair.setPlayer1(player1);
    pair.setPlayer2(player2);
    pair.setSeed(request.getSeed() != null ? request.getSeed() : 0);
    pair.setType(request.getType() != null ? request.getType() : io.github.redouanebali.model.PairType.NORMAL);

    return playerPairRepository.save(pair);
  }
}
