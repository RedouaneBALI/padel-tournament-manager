package io.github.redouanebali.controller;

import io.github.redouanebali.dto.GameUpdateRequest;
import io.github.redouanebali.dto.ScoreUpdateResponse;
import io.github.redouanebali.dto.TournamentDTO;
import io.github.redouanebali.dto.UpdatePlayerPairRequest;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
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

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TournamentDTO> createTournament(@RequestBody @Valid Tournament tournament) {
    Tournament saved = tournamentService.createTournament(tournament);
    return ResponseEntity
        .created(URI.create("/admin/tournaments/" + saved.getId()))
        .body(tournamentMapper.toDTO(saved));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
    checkOwnership(id);                 // même règle que pour update/draw/etc.
    tournamentService.deleteTournament(id);
    return ResponseEntity.noContent().build(); // 204 No Content
  }

  @GetMapping
  public List<TournamentDTO> listMyTournaments(@RequestParam(defaultValue = "mine") String scope) {
    String  me      = SecurityUtil.currentUserId();
    boolean isSuper = securityProps.getSuperAdmins().contains(me);

    List<Tournament> list = ("all".equalsIgnoreCase(scope) && isSuper)
                            ? tournamentService.listAll()
                            : tournamentService.listByOwner(me);

    return list.stream().map(tournamentMapper::toDTO).toList();
  }

  @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public TournamentDTO updateTournament(@PathVariable Long id, @RequestBody @Valid Tournament updated) {
    checkOwnership(id);
    return tournamentMapper.toDTO(tournamentService.updateTournament(id, updated));
  }

  @PostMapping(path = "/{id}/pairs", consumes = MediaType.APPLICATION_JSON_VALUE)
  public TournamentDTO addPairs(@PathVariable Long id, @RequestBody @Valid List<PlayerPair> players) {
    checkOwnership(id);
    return tournamentMapper.toDTO(playerPairService.addPairs(id, players));
  }

  @PatchMapping(path = "/{id}/pairs/{pairId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> updatePlayerPair(@PathVariable Long id,
                                               @PathVariable Long pairId,
                                               @RequestBody @Valid UpdatePlayerPairRequest req) {
    checkOwnership(id);

    playerPairService.updatePlayerPair(id, pairId, req.getPlayer1Name(), req.getPlayer2Name(), req.getSeed());
    return ResponseEntity.ok().build();
  }

  /**
   * @param manual if true players order is preserved, otherwise algorithm shuffles
   */
  @PostMapping(path = "/{id}/draw")
  public TournamentDTO generateDraw(@PathVariable Long id,
                                    @RequestParam(defaultValue = "false") boolean manual) {
    checkOwnership(id);
    return tournamentMapper.toDTO(tournamentService.generateDraw(id, manual));
  }

  @PutMapping(path = "/{id}/rounds/{stage}/match-format", consumes = MediaType.APPLICATION_JSON_VALUE)
  public MatchFormat updateMatchFormat(@PathVariable Long id,
                                       @PathVariable Stage stage,
                                       @RequestBody @Valid MatchFormat newFormat) {
    checkOwnership(id);
    return matchFormatService.updateMatchFormatForRound(id, stage, newFormat);
  }

  @PutMapping(path = "/{tournamentId}/games/{gameId}/score", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ScoreUpdateResponse updateScore(@PathVariable Long tournamentId,
                                         @PathVariable Long gameId,
                                         @RequestBody @Valid Score score) {
    checkOwnership(tournamentId);
    return gameService.updateGameScore(tournamentId, gameId, score);
  }

  @PutMapping(path = "/{tournamentId}/games/{gameId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ScoreUpdateResponse updateGame(@PathVariable Long tournamentId,
                                        @PathVariable Long gameId,
                                        @RequestBody @Valid GameUpdateRequest request) {
    checkOwnership(tournamentId);
    return gameService.updateGame(tournamentId, gameId, request);
  }

  @GetMapping("/admin/debug/auth")
  public Map<String, Object> auth(Authentication a) {
    return Map.of(
        "name", a == null ? null : a.getName(),
        "authenticated", a != null && a.isAuthenticated(),
        "authorities", a == null ? null : a.getAuthorities(),
        "details", a
    );
  }

  private void checkOwnership(Long tournamentId) {
    String      me          = SecurityUtil.currentUserId();
    Tournament  tournament  = tournamentService.getTournamentById(tournamentId);
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to modify this tournament");
    }
  }


}