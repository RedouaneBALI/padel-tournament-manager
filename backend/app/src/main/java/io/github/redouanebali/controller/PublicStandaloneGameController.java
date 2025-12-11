package io.github.redouanebali.controller;

import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.security.AuthorizationService;
import io.github.redouanebali.service.StandaloneGameService;
import io.github.redouanebali.service.VoteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for standalone games (read-only public access).
 */
@RestController
@RequestMapping(value = "/games", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
public class PublicStandaloneGameController {

  private final StandaloneGameService standaloneGameService;
  private final TournamentMapper      tournamentMapper;
  private final AuthorizationService  authorizationService;
  private final VoteService           voteService;

  @GetMapping("/{id}")
  public ResponseEntity<GameDTO> getGame(@PathVariable Long id, HttpServletRequest request) {
    log.info("Getting public game with ID: {}", id);
    Game    game = standaloneGameService.getGameById(id);
    GameDTO dto  = tournamentMapper.toDTO(game);

    // Add isEditable flag based on current user permissions
    String  userId  = io.github.redouanebali.security.SecurityUtil.currentUserId();
    boolean canEdit = authorizationService.canEditGame(game, userId);
    dto.setIsEditable(canEdit);

    // Add vote summary
    dto.setVotes(voteService.getVoteSummary(id, request));

    log.debug("Game {} - user {} - canEdit: {}", id, userId, canEdit);
    return ResponseEntity.ok(dto);
  }
}

