package io.github.redouanebali.controller;

import io.github.redouanebali.config.SecurityProps;
import io.github.redouanebali.config.SecurityUtil;
import io.github.redouanebali.dto.GameUpdateRequest;
import io.github.redouanebali.dto.ScoreUpdateResponse;
import io.github.redouanebali.dto.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.service.GameService;
import io.github.redouanebali.service.MatchFormatService;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/admin/tournaments")
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminTournamentController {

  private final TournamentService tournamentService;

  private final PlayerPairService playerPairService;

  private final GameService gameService;

  private final MatchFormatService matchFormatService;

  private final SecurityProps securityProps;

  private final TournamentMapper tournamentMapper;

  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public TournamentDTO createTournament(@RequestBody Tournament tournament) {
    Tournament saved = tournamentService.createTournament(tournament);
    return tournamentMapper.toDTO(saved);
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping
  public List<TournamentDTO> listMyTournaments(@RequestParam(defaultValue = "mine") String scope) {
    String  me      = SecurityUtil.currentUserId();
    boolean isSuper = securityProps.getSuperAdmins().contains(me);

    List<Tournament> list = ("all".equalsIgnoreCase(scope) && isSuper)
                            ? tournamentService.listAll()
                            : tournamentService.listByOwner(me);

    return list.stream().map(tournamentMapper::toDTO).toList();
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{id}")
  public TournamentDTO updateTournament(@PathVariable Long id, @RequestBody Tournament updated) {
    checkOwnership(id);
    return tournamentMapper.toDTO(tournamentService.updateTournament(id, updated));
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/pairs")
  public TournamentDTO addPairs(@PathVariable Long id, @RequestBody @Valid List<PlayerPair> players) {
    checkOwnership(id);
    return tournamentMapper.toDTO(playerPairService.addPairs(id, players));
  }

  /**
   * @param manual if true, the rounds will be generated using the players in the same order otherwise, the algorithm will be used
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/draw")
  // @todo replace boolean manual by a enum
  public TournamentDTO generateDraw(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean manual) {
    checkOwnership(id);
    return tournamentMapper.toDTO(tournamentService.generateDraw(id, manual));
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{id}/rounds/{stage}/match-format")
  public MatchFormat updateMatchFormat(@PathVariable Long id, @PathVariable Stage stage, @RequestBody MatchFormat newFormat) {
    checkOwnership(id);
    return matchFormatService.updateMatchFormatForRound(id, stage, newFormat);
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{tournamentId}/games/{gameId}/score")
  public ScoreUpdateResponse updateScore(@PathVariable Long tournamentId, @PathVariable Long gameId, @RequestBody Score score) {
    checkOwnership(tournamentId);
    return gameService.updateGameScore(tournamentId, gameId, score);
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{tournamentId}/games/{gameId}")
  public ScoreUpdateResponse updateGame(@PathVariable Long tournamentId,
                                        @PathVariable Long gameId,
                                        @RequestBody GameUpdateRequest request) {
    checkOwnership(tournamentId);
    return gameService.updateGame(tournamentId, gameId, request);
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
