package io.github.redouanebali.service;

import io.github.redouanebali.dto.response.VoteSummaryDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Vote;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.VoteRepository;
import io.github.redouanebali.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

  private final VoteRepository voteRepository;
  private final GameRepository gameRepository;

  /**
   * Casts a vote for a team in a game. Each voter can vote once per game, but can change their vote if the game hasn't started.
   */
  @Transactional
  public Vote vote(Long gameId, TeamSide teamSide, HttpServletRequest request) {
    Game game = gameRepository.findById(gameId)
                              .orElseThrow(() -> new IllegalArgumentException("Game not found"));
    if (game.isStarted()) {
      throw new IllegalStateException("Cannot vote after game has started");
    }

    String  voterId         = resolveVoterId(request);
    boolean isAuthenticated = SecurityUtil.currentUserId() != null;

    Optional<Vote> existingVote = voteRepository.findByGameIdAndVoterId(gameId, voterId);
    if (existingVote.isPresent()) {
      // Allow changing vote if game hasn't started and to a different team
      if (existingVote.get().getTeamSide() == teamSide) {
        throw new IllegalStateException("You have already voted for this team");
      }
      existingVote.get().setTeamSide(teamSide);
      existingVote.get().setCreatedAt(Instant.now());
      voteRepository.save(existingVote.get());
      log.debug("Vote updated: game={}, team={}, authenticated={}", gameId, teamSide, isAuthenticated);
      return existingVote.get();
    } else {
      Vote vote = new Vote(gameId, voterId, teamSide, isAuthenticated);
      vote = voteRepository.save(vote);
      log.debug("Vote recorded: game={}, team={}, authenticated={}", gameId, teamSide, isAuthenticated);
      return vote;
    }
  }

  /**
   * Gets the vote summary for a game including vote counts and current user's vote.
   */
  @Transactional(readOnly = true)
  public VoteSummaryDTO getVoteSummary(Long gameId, HttpServletRequest request) {
    long teamAVotes = voteRepository.countTeamAVotes(gameId);
    long teamBVotes = voteRepository.countTeamBVotes(gameId);

    TeamSide currentUserVote = null;
    if (request != null) {
      String         voterId  = resolveVoterId(request);
      Optional<Vote> userVote = voteRepository.findByGameIdAndVoterId(gameId, voterId);
      currentUserVote = userVote.map(Vote::getTeamSide).orElse(null);
    }

    return new VoteSummaryDTO(teamAVotes, teamBVotes, currentUserVote);
  }

  /**
   * Resolves the voter ID from the request. Uses email for authenticated users, IP+User-Agent hash for anonymous.
   */
  String resolveVoterId(HttpServletRequest request) {
    String userId = SecurityUtil.currentUserId();
    log.info("resolveVoterId - userId: {}", userId);
    if (userId != null) {
      return "user:" + userId;
    }

    // For anonymous, use a combination of IP, User-Agent, and a session ID to differentiate sessions
    String ip        = getClientIp(request);
    String userAgent = request.getHeader("User-Agent");
    String sessionId = request.getHeader("X-Session-Id"); // Expect front-end to send a unique session ID
    if (sessionId == null) {
      sessionId = "default"; // Fallback, but not ideal
    }
    String fingerprint = ip + "|" + (userAgent != null ? userAgent : "unknown") + "|" + sessionId;
    String voterId     = "anon:" + hash(fingerprint);
    log.info("resolveVoterId - anonymous voterId: {} (IP: {}, UA: {}, Session: {})", voterId, ip, userAgent, sessionId);
    return voterId;
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String hash(String input) {
    try {
      MessageDigest md     = MessageDigest.getInstance("SHA-256");
      byte[]        digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, 16);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
