package io.github.redouanebali.api.controller;

import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.dto.response.MatchFormatDTO;
import io.github.redouanebali.dto.response.PlayerPairDTO;
import io.github.redouanebali.dto.response.PoolRankingDTO;
import io.github.redouanebali.dto.response.RoundDTO;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.service.GroupRankingService;
import io.github.redouanebali.service.MatchFormatService;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

  private final TournamentService   tournamentService;
  private final PlayerPairService   playerPairService;
  private final GroupRankingService groupRankingService;
  private final MatchFormatService  matchFormatService;
  private final TournamentMapper    tournamentMapper;

  /**
   * Retrieves complete tournament information by ID. Returns all tournament details including configuration, dates, and metadata.
   *
   * @param id the tournament ID
   * @return ResponseEntity containing the tournament DTO
   * @throws IllegalArgumentException if tournament is not found
   */
  @GetMapping("/{id}")
  public ResponseEntity<TournamentDTO> getTournament(@PathVariable Long id) {
    log.debug("Public access to tournament {}", id);
    Tournament tournament = tournamentService.getTournamentById(id);
    return ResponseEntity.ok(tournamentMapper.toDTO(tournament));
  }

  /**
   * Retrieves all player pairs for a tournament. Can optionally exclude BYE pairs from the result.
   *
   * @param id the tournament ID
   * @param includeByes whether to include BYE pairs in the result (default: false)
   * @return ResponseEntity containing list of player pair DTOs
   * @throws IllegalArgumentException if tournament is not found
   */
  @GetMapping("/{id}/pairs")
  public ResponseEntity<List<PlayerPairDTO>> getPairs(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "false") boolean includeByes) {
    log.debug("Getting pairs for tournament {} (includeByes: {})", id, includeByes);
    List<PlayerPairDTO> pairs = tournamentMapper.toDTOPlayerPairList(
        playerPairService.getPairsByTournamentId(id, includeByes)
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
    log.debug("Getting rounds for tournament {}", id);
    Tournament tournament = tournamentService.getTournamentById(id);
    List<RoundDTO> rounds = tournamentMapper.toDTORoundList(
        tournament.getRounds().stream()
                  .sorted(Comparator.comparing(r -> r.getStage().getOrder()))
                  .collect(Collectors.toList())
    );
    return ResponseEntity.ok(rounds);
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
    log.debug("Getting games for tournament {} stage {}", id, stage);
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
    log.debug("Getting match format for tournament {} stage {}", id, stage);
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
    log.debug("Getting group rankings for tournament {}", id);
    List<PoolRankingDTO> rankings = tournamentMapper.toDTOPoolRankingList(
        GroupRankingService.getGroupRankings(tournamentService.getTournamentById(id))
    );
    return ResponseEntity.ok(rankings);
  }
}
