package io.github.redouanebali.controller;

import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.dto.response.MatchFormatDTO;
import io.github.redouanebali.dto.response.PlayerPairDTO;
import io.github.redouanebali.dto.response.PoolRankingDTO;
import io.github.redouanebali.dto.response.RoundDTO;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.dto.response.TournamentSummaryDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.security.AuthorizationService;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.MatchFormatService;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
import io.github.redouanebali.service.UserService;
import io.github.redouanebali.service.VoteService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Public REST controller for tournament data access. Provides read-only endpoints for tournament information without authentication requirements. All
 * endpoints are publicly accessible and return tournament data in DTO format.
 */
@RestController
@RequestMapping(
    value = "/tournaments",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}", allowCredentials = "true")
@RequiredArgsConstructor
@PermitAll
@Slf4j
public class PublicTournamentController {

  private final TournamentService    tournamentService;
  private final PlayerPairService    playerPairService;
  private final MatchFormatService   matchFormatService;
  private final TournamentMapper     tournamentMapper;
  private final AuthorizationService authorizationService;
  private final UserService          userService;
  private final VoteService          voteService;

  /**
   * Retrieves complete tournament information by ID. Returns all tournament details including configuration, dates, and metadata. If user is
   * authenticated, includes isEditable flag to indicate if user can modify the tournament.
   *
   * @param id the tournament ID
   * @return ResponseEntity containing the tournament DTO with isEditable flag
   * @throws IllegalArgumentException if tournament is not found
   */
  @GetMapping("/{id}")
  public ResponseEntity<TournamentDTO> getTournament(@PathVariable Long id) {
    Tournament    tournament = tournamentService.getTournamentById(id);
    TournamentDTO dto        = tournamentMapper.toDTO(tournament);
    String        userId     = SecurityUtil.currentUserId();
    boolean       canEdit    = authorizationService.canEditTournament(tournament, userId);
    dto.setIsEditable(canEdit);
    dto.setOrganizerName(userService.getUserNameByEmail(dto.getOwnerId()));
    return ResponseEntity.ok(dto);
  }

  /**
   * Retrieves all player pairs for a tournament. Can optionally exclude BYE pairs and QUALIFIER placeholders from the result.
   *
   * @param id the tournament ID
   * @param includeByes whether to include BYE pairs in the result (default: false)
   * @param includeQualified whether to include QUALIFIER placeholders in the result (default: false)
   * @return ResponseEntity containing list of player pair DTOs
   * @throws IllegalArgumentException if tournament is not found
   */
  @GetMapping("/{id}/pairs")
  public ResponseEntity<List<PlayerPairDTO>> getPairs(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "false") boolean includeByes,
                                                      @RequestParam(defaultValue = "false") boolean includeQualified) {
    List<PlayerPairDTO> pairs = tournamentMapper.toDTOPlayerPairList(
        playerPairService.getPairsByTournamentId(id, includeByes, includeQualified)
    );
    return ResponseEntity.ok(pairs);
  }

  /**
   * Retrieves all rounds for a tournament ordered by stage sequence. Returns rounds sorted from qualification stages to final.
   *
   * @param id the tournament ID
   * @return ResponseEntity containing list of round DTOs sorted by stage order
   * @throws IllegalArgumentException if tournament is not found
   */
  @GetMapping("/{id}/rounds")
  public ResponseEntity<List<RoundDTO>> getRounds(@PathVariable Long id) {
    Tournament tournament = tournamentService.getTournamentById(id);
    List<RoundDTO> rounds = tournamentMapper.toDTORoundList(
        tournament.getRounds().stream()
                  .sorted(Comparator.comparing(r -> r.getStage().getOrder()))
                  .toList()
    );
    return ResponseEntity.ok(rounds);
  }

  /**
   * Retrieves a single game by its ID within a tournament.
   *
   * @param tournamentId the tournament ID
   * @param gameId the game ID
   * @param request the HTTP request for vote identification
   * @return ResponseEntity containing the game DTO with round information
   * @throws IllegalArgumentException if tournament or game is not found
   */
  @GetMapping("/{tournamentId}/games/{gameId}")
  public ResponseEntity<GameDTO> getGame(@PathVariable Long tournamentId, @PathVariable Long gameId, HttpServletRequest request) {
    Tournament tournament = tournamentService.getTournamentById(tournamentId);

    // Find the game in all rounds and include the round information
    for (var round : tournament.getRounds()) {
      for (var game : round.getGames()) {
        if (game.getId() != null && game.getId().equals(gameId)) {
          GameDTO gameDTO = tournamentMapper.toDTOWithLightRound(game, round);
          gameDTO.setVotes(voteService.getVoteSummary(gameId, request));
          return ResponseEntity.ok(gameDTO);
        }
      }
    }

    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found with ID: " + gameId);
  }

  /**
   * Retrieves all games for a specific tournament stage/round. Returns games with current scores and match status.
   *
   * @param id the tournament ID
   * @param stage the tournament stage to get games from
   * @return ResponseEntity containing set of game DTOs for the specified stage
   * @throws IllegalArgumentException if tournament or stage round is not found
   */
  @GetMapping("/{id}/rounds/{stage}/games")
  public ResponseEntity<Set<GameDTO>> getGamesByStage(@PathVariable Long id, @PathVariable Stage stage) {
    Set<GameDTO> games = tournamentMapper.toDTOGameSet(
        tournamentService.getGamesByTournamentAndStage(id, stage)
    );
    return ResponseEntity.ok(games);
  }

  /**
   * Retrieves the match format configuration for a specific tournament stage. Returns format details like number of sets, games per set, and tiebreak
   * rules.
   *
   * @param id the tournament ID
   * @param stage the tournament stage to get match format from
   * @return ResponseEntity containing the match format DTO for the specified stage
   * @throws IllegalArgumentException if tournament or stage round is not found
   */
  @GetMapping("/{id}/rounds/{stage}/match-format")
  public ResponseEntity<MatchFormatDTO> getMatchFormat(@PathVariable Long id, @PathVariable Stage stage) {
    MatchFormatDTO format = tournamentMapper.toDTO(
        matchFormatService.getMatchFormatForRound(id, stage)
    );
    return ResponseEntity.ok(format);
  }

  /**
   * Retrieves group stage rankings for tournaments with group phases. Returns standings with wins, losses, sets, and qualification status.
   *
   * @param id the tournament ID
   * @return ResponseEntity containing list of pool ranking DTOs
   * @throws IllegalArgumentException if tournament is not found
   */
  @GetMapping("/{id}/groups/ranking")
  public ResponseEntity<List<PoolRankingDTO>> getGroupRankings(@PathVariable Long id) {
    List<PoolRankingDTO> rankings = tournamentMapper.toDTOPoolRankingList(
        Pool.getGroupRankings(tournamentService.getTournamentById(id))
    );
    return ResponseEntity.ok(rankings);
  }

  /**
   * Retrieves active tournaments for the home page. Returns a lightweight summary list of tournaments that are currently ongoing within the specified
   * date range and having at least one non-null game. If no dates are provided, defaults to J-3 to J+3.
   *
   * @param startDate optional start date for filtering tournaments (format: YYYY-MM-DD)
   * @param endDate optional end date for filtering tournaments (format: YYYY-MM-DD)
   * @return list of tournament summaries
   */
  @GetMapping("/active")
  public ResponseEntity<List<TournamentSummaryDTO>> getActiveTournaments(@RequestParam(required = false) LocalDate startDate,
                                                                         @RequestParam(required = false) LocalDate endDate) {
    List<Tournament> entities = tournamentService.getActiveTournaments(startDate, endDate);
    
    List<TournamentSummaryDTO> summaries = entities.stream()
                                                   .map(tournamentMapper::toSummaryDTO)
                                                   .toList();
    return ResponseEntity.ok(summaries);
  }
}
