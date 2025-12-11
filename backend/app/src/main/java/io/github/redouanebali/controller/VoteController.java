package io.github.redouanebali.controller;

import io.github.redouanebali.dto.request.VoteRequest;
import io.github.redouanebali.dto.response.VoteSummaryDTO;
import io.github.redouanebali.service.VoteService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for game voting functionality. Allows authenticated and anonymous users to vote for teams in games.
 */
@RestController
@RequestMapping(value = "/games", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
@PermitAll
public class VoteController {

  private final VoteService voteService;

  /**
   * Casts a vote for a team in a game. Each voter can vote once per game, but can change their vote if the game hasn't started.
   *
   * @param gameId the game ID
   * @param voteRequest the vote request containing the team side
   * @param request the HTTP request
   * @return the vote summary after voting
   */
  @PostMapping("/{gameId}/votes")
  public ResponseEntity<VoteSummaryDTO> vote(@PathVariable Long gameId,
                                             @Valid @RequestBody VoteRequest voteRequest,
                                             HttpServletRequest request) {
    log.info("Vote request for game {} - team {}", gameId, voteRequest.getTeamSide());
    try {
      voteService.vote(gameId, voteRequest.getTeamSide(), request);
      VoteSummaryDTO summary = voteService.getVoteSummary(gameId, request);
      return ResponseEntity.ok(summary);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  /**
   * Gets the vote summary for a game.
   *
   * @param gameId the game ID
   * @param request the HTTP request
   * @return the vote summary with counts and current user's vote
   */
  @GetMapping("/{gameId}/votes")
  public ResponseEntity<VoteSummaryDTO> getVotes(@PathVariable Long gameId, HttpServletRequest request) {
    log.debug("Getting votes for game {}", gameId);
    VoteSummaryDTO summary = voteService.getVoteSummary(gameId, request);
    return ResponseEntity.ok(summary);
  }
}
