package io.github.redouanebali.controller;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.dto.request.CreateTournamentRequest;
import io.github.redouanebali.dto.request.RoundRequest;
import io.github.redouanebali.dto.request.UpdateGameRequest;
import io.github.redouanebali.dto.request.UpdatePlayerPairRequest;
import io.github.redouanebali.dto.request.UpdateTournamentRequest;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.dto.response.UpdateScoreDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.GameService;
import io.github.redouanebali.service.MatchFormatService;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/admin/tournaments",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AdminTournamentController {

  private final TournamentService  tournamentService;
  private final PlayerPairService  playerPairService;
  private final GameService        gameService;
  private final MatchFormatService matchFormatService;
  private final SecurityProps      securityProps;
  private final TournamentMapper   tournamentMapper;

  /**
   * Creates a new tournament with the provided configuration. Validates the tournament structure if both format and config are provided.
   *
   * @param request the tournament creation request containing only allowed fields
   * @return ResponseEntity containing the created tournament DTO with 201 status
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TournamentDTO> createTournament(@RequestBody @Valid CreateTournamentRequest request) {
    log.info("Creating tournament '{}' for user: {}", request.getName(), SecurityUtil.currentUserId());
    Tournament tournament = tournamentMapper.toEntity(request);
    Tournament saved      = tournamentService.createTournament(tournament);
    return ResponseEntity
        .created(URI.create("/admin/tournaments/" + saved.getId()))
        .body(tournamentMapper.toDTO(saved));
  }

  /**
   * Deletes a tournament. Only the owner or super admins can delete a tournament.
   *
   * @param id the tournament ID to delete
   * @return ResponseEntity with 204 No Content status
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
    log.info("User {} attempting to delete tournament {}", SecurityUtil.currentUserId(), id);
    checkOwnership(id);
    tournamentService.deleteTournament(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Lists tournaments based on the scope parameter. Returns all tournaments for super admins when scope='all', otherwise returns user's own
   * tournaments.
   *
   * @param scope the scope filter - "all" for all tournaments (super admin only), "mine" for user's tournaments
   * @return ResponseEntity containing list of tournament DTOs
   */
  @GetMapping
  public ResponseEntity<List<TournamentDTO>> listMyTournaments(@RequestParam(defaultValue = "mine") String scope) {
    String  me      = SecurityUtil.currentUserId();
    boolean isSuper = securityProps.getSuperAdmins().contains(me);

    List<Tournament> list = ("all".equalsIgnoreCase(scope) && isSuper)
                            ? tournamentService.listAll()
                            : tournamentService.listByOwner(me);

    return ResponseEntity.ok(list.stream().map(tournamentMapper::toDTO).toList());
  }

  /**
   * Updates an existing tournament with new data. Only the owner or super admins can update a tournament.
   *
   * @param id the tournament ID to update
   * @param updated the updated tournament data
   * @return ResponseEntity containing the updated tournament DTO
   */
  @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TournamentDTO> updateTournament(@PathVariable Long id, @RequestBody @Valid UpdateTournamentRequest updated) {
    checkOwnership(id);
    return ResponseEntity.ok(tournamentMapper.toDTO(tournamentService.updateTournament(id, updated)));
  }

  /**
   * Adds player pairs to a tournament and clears existing game assignments. Automatically adds BYE pairs if needed to reach the main draw size.
   *
   * @param id the tournament ID
   * @param players list of player pairs to add
   * @return ResponseEntity containing the updated tournament DTO
   */
  @PostMapping(path = "/{id}/pairs", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TournamentDTO> addPairs(@PathVariable Long id, @RequestBody @Valid List<CreatePlayerPairRequest> players) {
    checkOwnership(id);
    return ResponseEntity.ok(tournamentMapper.toDTO(playerPairService.addPairs(id, players)));
  }

  /**
   * Updates an existing player pair's information (names and seed). BYE pairs cannot be modified.
   *
   * @param id the tournament ID
   * @param pairId the player pair ID to update
   * @param req the update request containing new player names and/or seed
   * @return ResponseEntity with 204 No Content status
   */
  @PatchMapping(path = "/{id}/pairs/{pairId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> updatePlayerPair(@PathVariable Long id,
                                               @PathVariable Long pairId,
                                               @RequestBody @Valid UpdatePlayerPairRequest req) {
    checkOwnership(id);

    playerPairService.updatePlayerPair(id, pairId, req.getPlayer1Name(), req.getPlayer2Name(), req.getSeed());
    return ResponseEntity.noContent().build();
  }

  /**
   * Reorders the complete list of player pairs for a tournament. Useful for manual draw management and seeding adjustments. Only the owner or super
   * admins can reorder pairs.
   *
   * @param id the tournament ID
   * @param pairIds ordered list of pair IDs (must contain all existing pair IDs)
   * @return ResponseEntity with no content on success
   */
  @PutMapping(path = "/{id}/pairs/reorder", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> reorderPlayerPairs(@PathVariable Long id,
                                                 @RequestBody @Valid List<Long> pairIds) {
    checkOwnership(id);

    playerPairService.reorderPlayerPairs(id, pairIds);
    return ResponseEntity.noContent().build();
  }

  /**
   * Updates the match format for a specific tournament round/stage. Only the owner or super admins can modify match formats.
   *
   * @param id the tournament ID
   * @param stage the tournament stage/round
   * @param newFormat the new match format configuration
   * @return ResponseEntity containing the updated match format
   */
  @PutMapping(path = "/{id}/rounds/{stage}/match-format", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MatchFormat> updateMatchFormat(@PathVariable Long id,
                                                       @PathVariable Stage stage,
                                                       @RequestBody @Valid MatchFormat newFormat) {
    checkOwnership(id);
    return ResponseEntity.ok(matchFormatService.updateMatchFormatForRound(id, stage, newFormat));
  }

  /**
   * Updates the score of a specific game and propagates winners if the game is finished. Only the tournament owner or super admins can update
   * scores.
   *
   * @param tournamentId the tournament ID
   * @param gameId the game ID to update
   * @param score the new score
   * @return ResponseEntity containing update result with finish status and winner information
   */
  @PutMapping(path = "/{tournamentId}/games/{gameId}/score", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UpdateScoreDTO> updateScore(@PathVariable Long tournamentId,
                                                    @PathVariable Long gameId,
                                                    @RequestBody @Valid Score score) {
    checkOwnership(tournamentId);
    return ResponseEntity.ok(gameService.updateGameScore(tournamentId, gameId, score));
  }

  /**
   * Updates a game's complete information including score, time, and court. Propagates winners if the game becomes finished after the update.
   *
   * @param tournamentId the tournament ID
   * @param gameId the game ID to update
   * @param request the update request containing score, time, and court information
   * @return ResponseEntity containing update result with finish status and winner information
   */
  @PutMapping(path = "/{tournamentId}/games/{gameId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UpdateScoreDTO> updateGame(@PathVariable Long tournamentId,
                                                   @PathVariable Long gameId,
                                                   @RequestBody @Valid UpdateGameRequest request) {
    checkOwnership(tournamentId);
    return ResponseEntity.ok(gameService.updateGame(tournamentId, gameId, request));
  }

  /**
   * Debug endpoint that returns current authentication information. Useful for testing authentication and authorization.
   *
   * @param a the current authentication object
   * @return ResponseEntity containing authentication details
   */
  @PreAuthorize("hasRole('SUPER_ADMIN') or authentication.name in @securityProps.superAdmins")
  @GetMapping("/debug/auth")
  public ResponseEntity<Map<String, Object>> auth(Authentication a) {
    return ResponseEntity.ok(Map.of(
        "name", a != null ? a.getName() : "anonymous",
        "authenticated", a != null && a.isAuthenticated(),
        "authorities", a != null ? a.getAuthorities().toString() : "none",
        "details", a != null ? a.getDetails() : "none"
    ));
  }

  /**
   * Generates a manual draw using user-provided initial rounds. Replaces the tournament structure with the provided rounds configuration.
   *
   * @param id the tournament ID
   * @param initialRounds optional list of initial rounds provided by the user
   * @return ResponseEntity containing the tournament DTO with generated draw
   */
  @PostMapping(path = "/{id}/draw/manual")
  public ResponseEntity<TournamentDTO> generateDrawManual(@PathVariable Long id,
                                                          @RequestBody(required = false) List<RoundRequest> initialRounds) {
    log.info("Generating manual draw for tournament {} by user {}", id, SecurityUtil.currentUserId());
    checkOwnership(id);
    return ResponseEntity.ok(tournamentMapper.toDTO(tournamentService.generateDrawManual(id, initialRounds)));
  }

  /**
   * Verifies that the current user has ownership rights over a tournament. Throws AccessDeniedException if the user is not the owner or a super
   * admin.
   *
   * @param tournamentId the tournament ID to check ownership for
   * @throws AccessDeniedException if the user lacks ownership rights
   */
  private void checkOwnership(Long tournamentId) {
    String      me          = SecurityUtil.currentUserId();
    Tournament  tournament  = tournamentService.getTournamentById(tournamentId);
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !Objects.equals(me, tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to modify this tournament");
    }
  }

}