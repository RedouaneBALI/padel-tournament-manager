package io.github.redouanebali.controller;

import io.github.redouanebali.dto.RoundDTO;
import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.service.TournamentService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments")
public class TournamentController {

  @Autowired
  private TournamentService tournamentService;

  @PostMapping
  public Tournament createTournament(@RequestBody Tournament tournament) {
    return tournamentService.createTournament(tournament);
  }

  @PutMapping("/{id}")
  public Tournament updateTournament(@PathVariable("id") Long tournamentId, @RequestBody Tournament tournament) {
    return tournamentService.updateTournament(tournamentId, tournament);
  }

  @PostMapping("/{id}/draw")
  public RoundDTO generateDraw(@PathVariable("id") Long tournamentId) {
    return new RoundDTO(tournamentService.generateDraw(tournamentId));
  }

  @PostMapping("/{id}/pairs")
  public int addPairsToTournament(@PathVariable("id") Long tournamentId, @RequestBody List<SimplePlayerPairDTO> playerPairs) {
    return tournamentService.addPairs(tournamentId, playerPairs);
  }

  @GetMapping("/{id}/pairs")
  public List<SimplePlayerPairDTO> getPairsByTournament(@PathVariable("id") Long tournamentId) {
    Tournament tournament = tournamentService.getTournamentById(tournamentId);
    return tournament.getPlayerPairs().stream()
                     .map(SimplePlayerPairDTO::fromPlayerPair)
                     .toList();
  }

  @GetMapping("/{id}")
  public Tournament getTournamentById(@PathVariable("id") Long tournamentId) {
    return tournamentService.getTournamentById(tournamentId);
  }

  @GetMapping("/{id}/rounds")
  public List<Round> getTournamentRounds(@PathVariable("id") Long tournamentId) {
    return tournamentService.getTournamentById(tournamentId).getRounds();
  }

}
