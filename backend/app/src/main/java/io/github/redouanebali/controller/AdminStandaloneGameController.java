package io.github.redouanebali.controller;

import io.github.redouanebali.dto.request.CreateStandaloneGameRequest;
import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.dto.response.UpdateScoreDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.StandaloneGameService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoints for standalone games (mirror of AdminTournamentController pattern).
 */
@RestController
@RequestMapping(value = "/admin/games", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AdminStandaloneGameController {

  private final StandaloneGameService standaloneGameService;
  private final TournamentMapper      tournamentMapper;
  private final SecurityProps         securityProps;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<GameDTO> createGame(@RequestBody @Valid CreateStandaloneGameRequest request) {
    log.info("Creating standalone game (admin)");
    Game created = standaloneGameService.createStandaloneGame(request);
    return ResponseEntity
        .created(URI.create("/admin/games/" + created.getId()))
        .body(tournamentMapper.toDTO(created));
  }

  @GetMapping
  public ResponseEntity<List<GameDTO>> listMyGames(@RequestParam(defaultValue = "mine") String scope) {
    String  me      = SecurityUtil.currentUserId();
    boolean isSuper = securityProps.getSuperAdmins().contains(me);

    List<Game> games = ("all".equalsIgnoreCase(scope) && isSuper)
                       ? standaloneGameService.getAllStandaloneGames()
                       : standaloneGameService.getStandaloneGamesByOwner(me);

    return ResponseEntity.ok(tournamentMapper.toDTOGameList(games));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
    log.info("User {} attempting to delete game {}", SecurityUtil.currentUserId(), id);
    checkOwnership(id);
    standaloneGameService.deleteGame(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping(path = "/{id}/game-point")
  public ResponseEntity<UpdateScoreDTO> incrementGamePoint(@PathVariable Long id,
                                                           @RequestParam TeamSide teamSide) {
    log.info("User {} increments point for standalone game {}", SecurityUtil.currentUserId(), id);
    checkOwnership(id);
    return ResponseEntity.ok(standaloneGameService.incrementGamePoint(id, teamSide));
  }

  @PatchMapping(path = "/{id}/undo-game-point")
  public ResponseEntity<UpdateScoreDTO> undoGamePoint(@PathVariable Long id) {
    log.info("User {} undoes point for standalone game {}", SecurityUtil.currentUserId(), id);
    checkOwnership(id);
    return ResponseEntity.ok(standaloneGameService.undoGamePoint(id));
  }

  private void checkOwnership(Long gameId) {
    String me = SecurityUtil.currentUserId();
    if (securityProps.getSuperAdmins().contains(me)) {
      return; // super-admin can do anything
    }
    Game   game  = standaloneGameService.getGameById(gameId);
    String owner = game.getCreatedBy();
    if (owner == null || !owner.equals(me)) {
      throw new AccessDeniedException("You are not allowed to modify this game");
    }
  }

}
