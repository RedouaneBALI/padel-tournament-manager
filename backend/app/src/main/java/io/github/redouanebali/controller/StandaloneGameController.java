package io.github.redouanebali.controller;

import io.github.redouanebali.dto.request.CreateStandaloneGameRequest;
import io.github.redouanebali.dto.request.UpdateGameRequest;
import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.service.StandaloneGameService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing standalone games (matches without tournaments). Provides endpoints to create, read, update, and delete simple matches
 * between player pairs.
 *
 * Public access: GET endpoints (read-only) Authentication required: POST, PUT, DELETE endpoints (modifications)
 */
@RestController
@RequestMapping(
    value = "/games",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@Slf4j
@RequiredArgsConstructor
public class StandaloneGameController {

  private final StandaloneGameService standaloneGameService;
  private final TournamentMapper      tournamentMapper;

  /**
   * Creates a new standalone game with two player pairs and a match format. Requires authentication.
   *
   * @param request the request containing team A, team B, and match format
   * @return ResponseEntity containing the created game DTO with 201 status
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<GameDTO> createGame(@RequestBody @Valid CreateStandaloneGameRequest request) {
    log.info("Creating standalone game");
    Game created = standaloneGameService.createStandaloneGame(request);
    return ResponseEntity
        .created(URI.create("/games/" + created.getId()))
        .body(tournamentMapper.toDTO(created));
  }

  /**
   * Lists all standalone games. Public access - no authentication required.
   *
   * @return ResponseEntity containing list of game DTOs
   */
  @GetMapping
  public ResponseEntity<List<GameDTO>> listGames() {
    log.info("Listing all standalone games");
    List<Game> games = standaloneGameService.getAllStandaloneGames();
    return ResponseEntity.ok(tournamentMapper.toDTOGameList(games));
  }

  /**
   * Retrieves a specific game by its ID. Public access - no authentication required.
   *
   * @param id the game ID
   * @return ResponseEntity containing the game DTO
   */
  @GetMapping("/{id}")
  public ResponseEntity<GameDTO> getGame(@PathVariable Long id) {
    log.info("Getting game with ID: {}", id);
    Game game = standaloneGameService.getGameById(id);
    return ResponseEntity.ok(tournamentMapper.toDTO(game));
  }

  /**
   * Updates a standalone game's complete information including score, scheduled time, and court. Requires authentication.
   *
   * @param id the game ID
   * @param request the update request containing score, time, and court information
   * @return ResponseEntity containing the updated game DTO
   */
  @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<GameDTO> updateGame(@PathVariable Long id,
                                            @RequestBody @Valid UpdateGameRequest request) {
    log.info("Updating game {}", id);
    Game updated = standaloneGameService.updateGame(id, request);
    return ResponseEntity.ok(tournamentMapper.toDTO(updated));
  }

  /**
   * Updates the score of a standalone game. Requires authentication.
   *
   * @param id the game ID
   * @param score the new score
   * @return ResponseEntity containing the updated game DTO
   */
  @PutMapping(path = "/{id}/score", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<GameDTO> updateScore(@PathVariable Long id,
                                             @RequestBody @Valid Score score) {
    log.info("Updating score for game {}", id);
    Game updated = standaloneGameService.updateGameScore(id, score);
    return ResponseEntity.ok(tournamentMapper.toDTO(updated));
  }

  /**
   * Deletes a standalone game. Requires authentication.
   *
   * @param id the game ID to delete
   * @return ResponseEntity with 204 No Content status
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
    log.info("Deleting game {}", id);
    standaloneGameService.deleteGame(id);
    return ResponseEntity.noContent().build();
  }
}

