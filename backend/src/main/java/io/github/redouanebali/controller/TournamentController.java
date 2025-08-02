package io.github.redouanebali.controller;

import io.github.redouanebali.dto.ScoreUpdateResponse;
import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.service.TournamentService;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments")
@CrossOrigin(origins = "*")
public class TournamentController {

  @Autowired
  private TournamentService tournamentService;

  @PostMapping
  public Tournament createTournament(@RequestBody Tournament tournament) {
    return tournamentService.createTournament(tournament);
  }

  @PutMapping("/{id}")
  public Tournament updateTournament(@PathVariable Long id, @RequestBody Tournament updated) {
    return tournamentService.updateTournament(id, updated);
  }

  @GetMapping("/{id}")
  public Tournament getTournament(@PathVariable Long id) {
    return tournamentService.getTournamentById(id);
  }

  @PostMapping("/{id}/players")
  public int addPlayers(@PathVariable Long id, @RequestBody List<SimplePlayerPairDTO> players) {
    return tournamentService.addPairs(id, players);
  }

  @PostMapping("/{id}/draw")
  public Tournament generateDraw(@PathVariable Long id) {
    return tournamentService.generateDraw(id);
  }

  @GetMapping("/{id}/rounds/{stage}/games")
  public Set<Game> getGamesByStage(@PathVariable Long id, @PathVariable Stage stage) {
    return tournamentService.getGamesByTournamentAndStage(id, stage);
  }

  @GetMapping("/{id}/rounds/{stage}/match-format")
  public MatchFormat getMatchFormat(@PathVariable Long id, @PathVariable Stage stage) {
    return tournamentService.getMatchFormatForRound(id, stage);
  }

  @PutMapping("/{id}/rounds/{stage}/match-format")
  public MatchFormat updateMatchFormat(@PathVariable Long id, @PathVariable Stage stage, @RequestBody MatchFormat newFormat) {
    return tournamentService.updateMatchFormatForRound(id, stage, newFormat);
  }

  @PutMapping("/{tournamentId}/games/{gameId}/score")
  public ScoreUpdateResponse updateScore(@PathVariable Long tournamentId, @PathVariable Long gameId, @RequestBody Score score) {
    return tournamentService.updateGameScore(tournamentId, gameId, score);
  }
}
