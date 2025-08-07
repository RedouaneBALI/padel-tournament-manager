package io.github.redouanebali.controller;

import io.github.redouanebali.dto.GameUpdateRequest;
import io.github.redouanebali.dto.ScoreUpdateResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/admin/tournaments")
@RestController
public class AdminTournamentController {

  @Autowired
  private TournamentService tournamentService;

  @Autowired
  private PlayerPairService playerPairService;

  @Autowired
  private GameService gameService;

  @Autowired
  private MatchFormatService matchFormatService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public Tournament createTournament(@RequestBody Tournament tournament) {
    return tournamentService.createTournament(tournament);
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{id}")
  public Tournament updateTournament(@PathVariable Long id, @RequestBody Tournament updated) {
    return tournamentService.updateTournament(id, updated);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/pairs")
  public Tournament addPairs(@PathVariable Long id, @RequestBody @Valid List<PlayerPair> players) {
    return playerPairService.addPairs(id, players);
  }

  /**
   * @param manual if true, the rounds will be generated using the players in the same order otherwise, the algorithm will be used
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/draw")
  public Tournament generateDraw(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean manual) {
    return tournamentService.generateDraw(id, manual);
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{id}/rounds/{stage}/match-format")
  public MatchFormat updateMatchFormat(@PathVariable Long id, @PathVariable Stage stage, @RequestBody MatchFormat newFormat) {
    return matchFormatService.updateMatchFormatForRound(id, stage, newFormat);
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{tournamentId}/games/{gameId}/score")
  public ScoreUpdateResponse updateScore(@PathVariable Long tournamentId, @PathVariable Long gameId, @RequestBody Score score) {
    return gameService.updateGameScore(tournamentId, gameId, score);
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{tournamentId}/games/{gameId}")
  public ScoreUpdateResponse updateGame(@PathVariable Long tournamentId,
                                        @PathVariable Long gameId,
                                        @RequestBody GameUpdateRequest request) {
    return gameService.updateGame(tournamentId, gameId, request);
  }

}
